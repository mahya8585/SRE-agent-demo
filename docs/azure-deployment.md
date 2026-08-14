# Azure本番デプロイ

本番環境は、サブスクリプションスコープのオーケストレーター`infra/main.bicep`と、`infra/modules`配下のリソースグループスコープモジュールで定義します。

## リソース

- リソースグループ`SREagent-lab`
- StoreとAPIを実行するVNet統合済みAzure Container Apps環境
- マネージドIDでイメージを取得するBasic Azure Container Registry
- プライベートPostgreSQL Flexible ServerとPrivate DNS
- `Standard_ZRS`のStorage Account、非公開`wine-images` Blobコンテナー、Blob Private EndpointとPrivate DNS
- RBAC、消去保護、90日間の論理削除を設定したKey Vault
- Log AnalyticsとワークスペースベースのApplication Insights
- Australia EastのAzure SRE Agent PaaSと専用Azure Monitor Workspace
- SRE Agentのシステム割り当てマネージドIDに付与する読み取り専用の`Monitoring Reader`ロールと`Log Analytics Reader`ロール

Operation Pulseはデプロイしません。以前使用していたコンテナーアプリ、Entraアプリ登録、ACRリポジトリ`operation-pulse`は2026年8月13日に削除済みです。

`admin-frontend/Dockerfile`で管理画面のイメージはビルドできますが、現在の`infra/main.bicep`、`infra/modules/container-apps.bicep`、`infra/deploy.ps1`は管理画面をデプロイしません。管理画面用のContainer App、ACRビルド、公開URLは未定義です。管理APIはAPIイメージに含まれますが、2026年8月13日の本番検証後に追加された現在の管理APIコードが本番へ反映済みであることは未検証です。

アプリケーションワークロードはJapan Eastに配置します。`Microsoft.Monitor/observabilityAgents`をJapan Eastで利用できないため、Azure SRE AgentはAustralia Eastへ配置します。エージェントは手動モードを使用し、調査と軽減策の提案は行いますが、変更を自動適用しません。Azure SRE Agentが存在する間は、Azure Agent Unitの常時料金が発生します。

## 検証

```powershell
az bicep build --file infra/main.bicep
./infra/deploy.ps1 -ValidateOnly
./infra/deploy.ps1 -WhatIf
```

1つ目のコマンドはローカルのみでBicepをコンパイルします。2つ目はAzureデプロイを検証し、3つ目はAzureへの変更内容をプレビューします。どちらのスクリプトモードもワークロードリソースを作成しません。

## デプロイ

```powershell
./infra/deploy.ps1 -ResourceGroupName SREagent-lab -CostCenter demo
```

スクリプトはPostgreSQLパスワードをメモリ上で生成し、シークレットを一時パラメーターファイルにだけ保存して、`finally`で削除します。ブートストラップアプリをデプロイし、ACRで変更不可能なStoreとAPIのイメージをビルドして、最終アプリリビジョンを適用します。管理画面のイメージはビルドしません。

サインインユーザーにはロール割り当てを作成する権限が必要です。サービスプリンシパルで実行する場合は、そのオブジェクトIDを`-DeploymentPrincipalId`へ渡します。

デプロイはAzure Resource Managerの増分モードを使用します。Bicepからリソース宣言を削除しても、デプロイ済みリソースは削除されません。コンポーネントを廃止する場合は、最初に新しい構成を適用して維持対象のワークロードを確認し、その後で廃止対象のリソースだけを明示的に削除します。

完了済みのOperation Pulse廃止作業では、次のリソースを明示的に削除しました。

- コンテナーアプリ`azpuldepgzxcukhrdm`
- 表示名が完全一致するEntraアプリ登録`Operation Pulse - SREagent-lab`
- ACRリポジトリ`operation-pulse`

テナントレベルのEntraオブジェクトを削除する前に、完全一致で検索して結果を確認します。

```powershell
$apps = az ad app list --display-name 'Operation Pulse - SREagent-lab' `
    --query "[?displayName=='Operation Pulse - SREagent-lab'].{id:id,appId:appId,displayName:displayName}" `
    --output json | ConvertFrom-Json

if (@($apps).Count -ne 1) {
    throw "Operation Pulseのアプリ登録は完全一致で1件必要です。検出件数: $(@($apps).Count)"
}

az ad app delete --id $apps[0].id
```

現在のIaCとデプロイスクリプトは、これらのPulseリソースを再作成しません。

## 運用

APIが`production`プロファイルで起動すると、Liquibaseがバージョン管理されたデータベース変更を適用します。PostgreSQLはプライベート構成であり、常設の管理VMはデプロイしません。復旧にはAzureコントロールプレーンの復元操作を使用し、例外的なSQLアクセスには承認済みの一時的なVNet接続ジョブを使用します。

商品画像はPostgreSQLではなくBlob Storageへ保存します。Storage Accountは共有キー、匿名Blobアクセス、パブリックネットワークアクセスを無効化します。APIのユーザー割り当てマネージドIDにStorage Accountスコープの`Storage Blob Data Contributor`を付与し、Container Appへ次の環境変数を設定します。

| 環境変数 | 内容 |
| --- | --- |
| `AZURE_STORAGE_BLOB_ENDPOINT` | Private Endpointで名前解決するBlobサービスURL |
| `AZURE_STORAGE_BLOB_CONTAINER_NAME` | `wine-images` |
| `AZURE_CLIENT_ID` | API用ユーザー割り当てマネージドIDのクライアントID |

Blobは7日間の論理削除を設定します。APIはBlobを読み取り、`/api/wine-images/{id}`から365日のキャッシュヘッダー付きで返します。ブラウザーへStorage Account URL、SAS、アクセスキーは公開しません。

低コスト構成では、意図的にスケールゼロ、単一ゾーンのBurstableデータベース、7日間のローカルバックアップを使用し、WAFと有料DDoSプランは使用しません。目標復旧時間は手動で4時間以内、目標復旧時点は最大1時間前です。販売サイトを業務上重要なシステムとして扱う前に、HAとバックアップ設定を強化してください。

## 現在の本番エンドポイント

| コンポーネント | エンドポイント |
| --- | --- |
| Store | <https://azstodepgzxcukhrdm.livelyfield-29cce79e.japaneast.azurecontainerapps.io> |
| API | <https://azapidepgzxcukhrdm.livelyfield-29cce79e.japaneast.azurecontainerapps.io> |

デプロイ済みContainer Appsは、`minReplicas: 0`を設定した`Consumption`ワークロードプロファイルを明示的に使用します。アプリケーションのターゲットポートはStoreが8080、APIが8081です。

管理画面の本番エンドポイントはありません。ローカルでは`http://localhost:3001`からAPIへ接続できますが、管理画面と`/api/admin/**`には認証・認可が未実装です。管理画面を外部公開する前にMicrosoft Entra IDなどで両方を保護し、管理画面のオリジンをAPIの`CORS_ALLOWED_ORIGINS`へ追加してください。

## アプリケーションテレメトリ

テレメトリ設計、プライバシー上の制約、メトリック定義、確認用クエリの詳細は、[オブザーバビリティ設計](observability.md)を参照してください。

APIイメージはApplication Insights Javaエージェント3.7.8を実行します。既存の環境変数`APPLICATIONINSIGHTS_CONNECTION_STRING`により、次のテレメトリをワークスペースベースのApplication Insightsリソースへ送信します。

- HTTP要求、応答時間、ステータスコード、分散トレースの相関情報
- エージェントがクエリリテラルをマスクしたJDBC依存関係
- 計装から検出可能な処理済み例外と未処理例外
- `INFO`以上のLogbackログ
- JVMのCPU、メモリ、ガベージコレクション、クラス、スレッドのメトリック
- Micrometer業務メトリック: `wine_orders_confirmed`、`wine_orders_stock_rejected`、`wine_orders_total`、`wine_orders_items`、および`wine_admin_`で始まる5つの管理操作カウンター

注文ログには、生成された注文番号、商品数、送料区分、合計金額を含めます。在庫不足ログには、ワインIDと在庫数量を含めます。管理操作ログには注文・商品・発注の識別子と変更前後の非個人情報を含めます。顧客名、メールアドレス、配送先住所はログへ出力せず、テレメトリディメンションにも追加しません。

管理画面を別途ビルドする場合、`VITE_APPLICATIONINSIGHTS_CONNECTION_STRING`を設定するとBrowser SDKがページ表示、Fetch依存関係、JavaScript例外、管理操作の成功、API失敗をApplication Insightsへ送信します。この値はViteのビルド成果物へ埋め込まれます。現在のデプロイスクリプトは管理画面をビルドしないため、このブラウザ設定も適用しません。

コンテナーの標準出力と標準エラーは、Container Apps環境を通じて引き続きLog Analyticsへ個別に送信されます。Logbackは標準出力へ書き込み、Javaエージェントも同じログを収集するため、アプリケーションログはContainer AppsのコンソールログテーブルとApplication Insightsの`traces`の両方で確認できます。

APIトラフィックを発生させてから数分待ち、次のApplication Insightsクエリで取り込みを確認します。

```kusto
requests
| where timestamp > ago(30m)
| where cloud_RoleName == "wine-api"
| project timestamp, name, resultCode, success, duration, operation_Id
| order by timestamp desc
```

```kusto
traces
| where timestamp > ago(30m)
| where cloud_RoleName == "wine-api"
| where customDimensions.["event.name"] in ("OrderConfirmed", "OrderRejectedOutOfStock")
| project timestamp, severityLevel, message, customDimensions, operation_Id
| order by timestamp desc
```

```kusto
customMetrics
| where timestamp > ago(30m)
| where cloud_RoleName == "wine-api"
| where name startswith "wine_orders_"
| summarize value=sum(value) by name, bin(timestamp, 5m)
| order by timestamp desc
```

## 検証記録

Operation Pulse削除後の本番状態を2026年8月13日に確認しました。

| 確認項目 | 結果 |
| --- | --- |
| BicepビルドとAzure検証 | 成功 |
| 最終サブスクリプションデプロイ | 成功 |
| Storeへの要求 | HTTP 200 |
| `GET /api/wines` | HTTP 200、6商品 |
| 廃止済み`GET /api/demo/incidents` | HTTP 404 |
| Container Apps | StoreとAPIのみ |
| Azure SRE Agent | Australia Eastで`azsredepgzxcukhrdm`を維持 |
| Operation PulseのEntra登録 | 0件 |
| ACRリポジトリ`operation-pulse` | なし |

管理機能とBlob Storage対応後の2026年8月14日の最新実行では、バックエンドの24テストが成功し、失敗・エラー・スキップはありません。管理画面と販売サイトのローカルビルド、Bicepコンパイルも成功しています。

上表は2026年8月13日の本番デプロイを対象とします。当日はローカルの販売サイトビルドがnpmレジストリ応答待ちにより完了できませんでしたが、ACRでの販売サイトイメージビルド、デプロイ、本番環境のHTTP確認は成功しました。翌日に追加した管理画面と管理APIについては、本番への反映を検証していません。

デプロイ、ACRビルド、Container Apps、Azure CLIで問題が発生した場合は、[トラブルシューティングガイド](troubleshooting.md)を参照してください。
