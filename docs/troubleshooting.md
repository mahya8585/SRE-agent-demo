# トラブルシューティングガイド

## 目的

この文書は、Maison Vigneのローカル開発、テスト、Application Insights、Azure Container Apps、ACRビルド、Bicepデプロイで発生した問題の切り分けと復旧手順をまとめたものです。

コマンドは特記がない限り、リポジトリルートでWindows PowerShell 5.1から実行します。運用中のAzureリソースを変更する前に、読み取り専用の確認、`-ValidateOnly`、`-WhatIf`の順で影響を確認してください。

## 5分で行う初動確認

1. 影響範囲を確認します。
   - Storeだけか、管理画面だけか、APIも失敗しているか
   - ローカルだけか、Azure本番環境でも再現するか
   - 全要求か、特定のAPIや管理操作だけか
2. 直前の変更を確認します。

   ```powershell
   git status --short
   git diff --stat
   ```

3. ローカルの基本動作を確認します。

   ```powershell
   Invoke-WebRequest http://localhost:8081/api/wines -UseBasicParsing
   Invoke-WebRequest http://localhost:3000 -UseBasicParsing
   Invoke-WebRequest http://localhost:3001 -UseBasicParsing
   ```

4. Azure CLIの接続先を確認します。

   ```powershell
   az account show --output table
   az resource list --resource-group SREagent-lab --output table
   ```

5. Azure障害の場合は、リソース正常性、アクティビティログ、Container Appsログ、Application Insightsの順で確認します。

   ```powershell
   az monitor activity-log list --resource-group SREagent-lab --max-events 20 --output table
   ```

調査時刻、再現手順、HTTPステータス、対象URL、Container Appのリビジョン名、デプロイ名、ACR Run IDを記録します。接続文字列、パスワード、トークン、顧客名、メールアドレス、配送先住所、要求本文は記録しません。

## 症状別早見表

| 症状 | 最初に確認する場所 | 主な原因 |
| --- | --- | --- |
| APIが起動しない | APIコンソール、`mvn test` | コンパイル、ポート競合、設定、DB初期化 |
| UIからAPIを呼べない | ブラウザー開発者ツール、API URL、CORS | API未起動、URL誤り、許可origin不足 |
| 管理画面が本番URLで開かない | Adminリビジョン、ACRイメージ、Ingress | イメージビルド失敗、起動失敗、ターゲットポート不一致 |
| 商品画像を登録・表示できない | API応答、Blob Storage、ファイル形式 | 形式・サイズ違反、マネージドID、Private DNS、RBAC |
| Application Insightsに何もない | 接続文字列、Javaエージェント、取り込み待ち | 接続文字列だけ設定、エージェント未起動、対象トラフィックなし |
| ブラウザテレメトリだけない | Vite環境変数、ビルド時設定 | 接続文字列未設定、設定後に未再起動・未再ビルド |
| 停止ログがない | Container Appsシステムログ、再起動数 | `SIGKILL`、OOM Kill、ノード障害 |
| Dockerビルドできない | `docker version` | Docker DesktopのLinuxエンジン停止 |
| npmが進まない | npmレジストリ疎通 | ネットワーク、プロキシ、レジストリ応答待ち |
| Azure CLIの検索結果が空 | サブスクリプション、リソースグループ | CLIコンテキスト違い、Resource Graph反映差 |
| Azure CLIの`--query`が失敗 | PowerShellの引用符 | JMESPath式の特殊文字がシェルで解釈された |
| 初回アクセスだけ遅い | Container Appのレプリカ数 | `minReplicas: 0`によるコールドスタート |
| 削除したはずのリソースが残る | ARMデプロイモード | 増分デプロイは既存リソースを削除しない |

## ローカル開発

### APIが起動しない

#### 確認

```powershell
Set-Location backend
mvn test
mvn spring-boot:run
```

APIは既定で`8081`を使用します。ポートの使用状況を確認します。

```powershell
Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue |
    Select-Object LocalAddress, LocalPort, State, OwningProcess
```

#### 切り分け

- コンパイルまたはテスト失敗: 最初に表示された原因例外を修正し、`mvn test`を再実行します。
- `Address already in use`: `OwningProcess`を確認し、不要な既存APIだけを停止します。
- ローカルDB初期化失敗: `application.properties`を確認します。ローカルではH2とJPAスキーマ生成を使用し、Liquibaseは無効です。
- 本番プロファイルだけ失敗: PostgreSQL、Key Vault、環境変数、LiquibaseをContainer Appsログで確認します。

### Storeまたは管理画面からAPIを呼べない

API自体を直接確認します。

```powershell
Invoke-RestMethod http://localhost:8081/api/wines
Invoke-RestMethod http://localhost:8081/api/admin/inventory
```

直接呼び出せる場合は、ブラウザーのNetworkタブで要求URL、HTTPステータス、CORSエラーを確認します。

- Storeの既定URL: `http://localhost:3000`
- 管理画面の既定URL: `http://localhost:3001`
- APIの既定URL: `http://localhost:8081`
- UIの接続先: `VITE_API_BASE_URL`
- APIの許可origin: `CORS_ALLOWED_ORIGINS`

ローカルの既定CORS設定はStoreと管理画面を許可します。環境変数を変更した場合はAPIを再起動します。Vite環境変数を変更した場合は、開発サーバーを再起動するか本番成果物を再ビルドします。

### npmの依存関係取得が進まない

過去にローカルの販売サイトビルドがnpmレジストリ応答待ちで完了しない事象がありました。

```powershell
Get-Command node, npm
npm config get registry
npm ping
```

プロキシや組織ネットワークの設定を確認した後、対象ディレクトリで再実行します。

```powershell
npm install
npm run build
```

`node_modules`やロックファイルを無条件に削除すると依存関係が変わるため、原因が確認できるまでは削除しません。StoreとAPIのAzureデプロイが目的で、ローカルDockerまたはnpm取得だけが失敗している場合は、`infra/deploy.ps1`が行うACR上のLinuxビルドで検証できます。管理画面は現在のデプロイスクリプトに含まれないため、この代替経路ではビルドされません。

### Docker DesktopでLinuxイメージをビルドできない

過去にDocker DesktopのLinuxエンジンが停止しており、ローカルのイメージ検証ができない事象がありました。

```powershell
docker version
docker info
```

Server情報を取得できない場合はDocker Desktopを起動し、Linux containersへ切り替えてから再実行します。

```powershell
docker build backend -t wine-api:local
docker build frontend --build-arg SITE=ops --build-arg VITE_API_BASE_URL=http://localhost:8081 -t maison-vigne-store:local
```

ローカルエンジンを利用できない場合でも、StoreとAPIはAzure Container Registryビルドで検証できます。ACRビルドにはAzureへのサインイン、対象サブスクリプションへの権限、ネットワーク接続が必要です。

## Application Insightsとログ

### 接続文字列を設定してもAPIテレメトリが届かない

Application Insights接続文字列だけではJavaアプリケーションは計装されません。この問題に対して、APIイメージへApplication Insights Javaエージェント3.7.8を追加済みです。

#### 確認項目

1. `backend/Dockerfile`がエージェントを取得し、SHA-256を検証している。
2. JVMが`-javaagent:/app/applicationinsights-agent.jar`付きで起動している。
3. `backend/applicationinsights.json`がイメージへ含まれている。
4. Container Appに`APPLICATIONINSIGHTS_CONNECTION_STRING`が設定されている。
5. APIへ確認用トラフィックを送信した後、数分待っている。

接続文字列そのものは表示せず、設定の存在だけを確認します。

```powershell
$ApiApp = '<API_CONTAINER_APP_NAME>'
az containerapp show --name $ApiApp --resource-group SREagent-lab --output json |
    ConvertFrom-Json |
    Select-Object -ExpandProperty properties |
    Select-Object -ExpandProperty template |
    Select-Object -ExpandProperty containers |
    Select-Object -ExpandProperty env |
    Select-Object name, secretRef
```

値が`secretRef`ではなく直接設定される構成では、コマンド出力に接続文字列が含まれる可能性があります。その場合は共有ログへ貼り付けません。

エージェント自身の警告はContainer Appsコンソールログで確認します。

```kusto
ContainerAppConsoleLogs_CL
| where TimeGenerated > ago(30m)
| where ContainerAppName_s startswith "azapi"
| where Log_s has_any ("Application Insights", "applicationinsights")
| project TimeGenerated, RevisionName_s, Log_s
| order by TimeGenerated desc
```

### ログが見つからない、または二重に見える

送信先は用途ごとに異なります。

- Container Appsの標準出力・標準エラー: Log Analyticsの`ContainerAppConsoleLogs_CL`
- HTTP要求: Application Insightsの`requests`
- JDBC呼び出し: `dependencies`
- Logbackログ: `traces`または`exceptions`
- Micrometerメトリック: `customMetrics`
- 管理画面のイベント: `customEvents`

Logbackは標準出力へ書き込み、Javaエージェントも同じログを収集するため、アプリケーションログがLog AnalyticsとApplication Insightsの両方に現れるのは想定どおりです。

最初にAPI要求の存在を確認し、同じ`operation_Id`でログと例外を追跡します。

```kusto
requests
| where timestamp > ago(30m)
| where cloud_RoleName == "wine-api"
| project timestamp, name, resultCode, success, duration, operation_Id
| order by timestamp desc
```

```kusto
union traces, (exceptions | extend message = outerMessage)
| where timestamp > ago(30m)
| where cloud_RoleName == "wine-api"
| project timestamp, itemType, severityLevel, message, operation_Id, customDimensions
| order by timestamp desc
```

### カスタムメトリック名がコードと一致しない

Application Insights Javaエージェントは、Micrometerメトリック名のピリオドをアンダースコアへ変換します。

- コード: `wine.orders.confirmed`
- Application Insights: `wine_orders_confirmed`

```kusto
customMetrics
| where timestamp > ago(30m)
| where name startswith "wine_orders_" or name startswith "wine_admin_"
| summarize value=sum(value) by name, bin(timestamp, 5m)
| order by timestamp desc
```

### 正常停止ログが記録されない

`ApplicationStopping`は、JVMがSpringの停止処理を実行できた場合にだけ記録されます。`SIGKILL`、OOM Kill、ノード障害では、停止ログとApplication Insightsのバッファ送信を保証できません。

この場合は次を確認します。

- Container Appsのシステムログとコンソールログ
- リビジョンとレプリカの再起動状況
- メモリ、CPU、要求失敗率
- Azure Resource Healthとアクティビティログ
- `ApplicationReady`が再度記録された時刻

停止ログがないことだけを根拠に、正常稼働またはテレメトリ欠落と判断しません。

### 管理画面のブラウザテレメトリだけ届かない

Browser SDKは`VITE_APPLICATIONINSIGHTS_CONNECTION_STRING`が設定されている場合だけ初期化します。この変数はViteのビルド時に成果物へ埋め込まれます。

```powershell
Set-Location admin-frontend
$env:VITE_APPLICATIONINSIGHTS_CONNECTION_STRING = '<CONNECTION_STRING>'
npm run build
```

接続文字列は認証シークレットではありませんが、公開JavaScriptへ含まれます。APIキーや資格情報を同じ変数へ設定しません。設定変更後は開発サーバーの再起動または再ビルドが必要です。

Azure上の管理画面でページ表示やイベントがない場合は、Admin Container Appの最新リビジョン、イメージタグ、HTTP応答を確認します。接続文字列はデプロイスクリプトがBase64化してACRビルドへ渡し、ビルドコンテナー内で復号します。

## Azure CLIとリソース検索

### Resource Graphでは空だがAzure CLIではリソースが見える

過去にResource Graphで`SREagent-lab`の結果が返らず、Azure CLIではリソースを確認できる事象がありました。反映遅延、対象テナント・サブスクリプション、クエリ条件の違いを切り分けます。

```powershell
az account show --output table
az account list --output table
az group show --name SREagent-lab --output table
az resource list --resource-group SREagent-lab --output table
```

CLIとResource Graphの結果が異なる場合は、対象サブスクリプションIDを記録し、リソースグループを直接指定するAzure Resource Managerの結果を一次情報として確認します。Resource Graphの空結果だけを根拠に、リソースが存在しないと判断しません。

### WindowsでJMESPathクエリが解析エラーになる

PowerShellや`cmd.exe`では、引用符、角括弧、疑問符などがJMESPathより先にシェルで解釈される場合があります。複雑な`--query`を避け、JSONをPowerShellで解析します。

```powershell
$resources = az resource list --resource-group SREagent-lab --output json |
    ConvertFrom-Json

$resources |
    Where-Object { $_.type -eq 'Microsoft.App/containerApps' } |
    Select-Object name, location, resourceGroup
```

単純な`--query`を使用する場合も、PowerShellでは式全体を引用符で囲みます。秘密情報を含むJSONをファイルへ保存しないでください。

### サブスクリプションまたは認証コンテキストが違う

```powershell
az account show --output table
az account set --subscription '<SUBSCRIPTION_ID_OR_NAME>'
az account show --output table
```

現在この環境で使用するサブスクリプションは、デプロイ前に必ず利用者が確認します。サービスプリンシパルで`infra/deploy.ps1`を実行する場合は、`-DeploymentPrincipalId`へオブジェクトIDを明示します。

## Azureデプロイ

### デプロイ前の確認

いきなり本番デプロイせず、次の順で実行します。

```powershell
az bicep build --file infra/main.bicep
./infra/deploy.ps1 -ResourceGroupName SREagent-lab -ValidateOnly
./infra/deploy.ps1 -ResourceGroupName SREagent-lab -WhatIf
```

- Bicepビルド失敗: 構文、型、モジュール参照を確認します。
- Azure検証失敗: プロバイダー、リージョン、SKU、RBAC、ポリシー、クォータを確認します。
- What-Ifに意図しない削除・再作成がある: デプロイせず、IaC差分を修正します。
- ロール割り当て失敗: 実行主体にロール割り当てを作成する権限があるか確認します。
- `ServerStoppedError`: PostgreSQL Flexible Serverを起動し、`Ready`を確認してからWhat-Ifを再実行します。

### ACRビルドが失敗する

`infra/deploy.ps1`はACRビルドのRun IDを取得し、最大30分待機して最終状態を検証します。エラーにはRun IDが表示されるため、対象実行を確認します。

```powershell
$RegistryName = '<ACR_NAME>'
az acr task list-runs --registry $RegistryName --top 10 --output table
az acr task logs --registry $RegistryName --run-id '<RUN_ID>'
```

主な確認点:

- Dockerfileのパスとビルドコンテキスト
- Application Insights JavaエージェントのダウンロードとSHA-256検証
- npmまたはMavenの依存関係取得
- ビルド引数にセミコロンなどのシェル特殊文字を直接渡していないか
- lockfileの`resolved`が開発環境固有のレジストリを指していないか
- `.dockerignore`でローカル`node_modules`と`dist`を除外しているか
- ACRの権限、ネットワーク、クォータ
- 同じイメージタグの扱い

スクリプトは時刻ベースの変更不可能なタグを既定で使用します。失敗したRun IDとイメージタグを調査記録へ残します。

### デプロイは成功したが新しいコードが見えない

```powershell
$ApiApp = '<API_CONTAINER_APP_NAME>'
az containerapp show --name $ApiApp --resource-group SREagent-lab --output table
az containerapp revision list --name $ApiApp --resource-group SREagent-lab --output table
```

次を照合します。

- デプロイ出力の`ImageTag`
- 最新リビジョンが参照するイメージタグ
- 最新リビジョンのプロビジョニング状態とトラフィック
- APIの`ApplicationReady`時刻
- 実際に呼び出したURLとHTTP応答

2026年8月13日の本番検証はStoreと当時のAPIを対象とします。翌日に追加した管理APIの本番反映は未検証です。過去の検証成功を、現在のコードがデプロイ済みである根拠にしません。

### 管理画面を開けるがAPI操作を実行できない

Admin Container App、API、CORSを順に確認します。APIの`CORS_ALLOWED_ORIGINS`にはStoreとAdminの完全なHTTPSオリジンが必要です。管理画面と`/api/admin/**`には認証・認可が未実装のため、現在の公開環境はデモ用途に限定します。実運用前にMicrosoft Entra IDなどで両方を保護します。

### Bicepから削除したリソースがAzureに残る

このデプロイはAzure Resource Managerの増分モードです。テンプレートから宣言を削除しても、既存リソースは自動削除されません。

1. 新しい構成をデプロイします。
2. 維持対象が正常であることを確認します。
3. 削除対象を名前とリソースIDで完全一致確認します。
4. 依存関係と影響を確認します。
5. 対象だけを明示削除します。

Operation Pulse廃止時もこの手順でContainer App、Entraアプリ登録、ACRリポジトリを明示削除しました。あいまいな名前検索の結果を一括削除しません。

## Container Apps本番障害

### コールドスタートまたは初回要求の遅延

StoreとAPIは`minReplicas: 0`でスケールゼロを許可します。アイドル後の初回アクセスが遅い場合は、後続要求との応答時間差、レプリカ起動時刻、`ApplicationReady`を確認します。

```powershell
1..3 | ForEach-Object {
    Measure-Command {
        Invoke-WebRequest '<API_URL>/api/wines' -UseBasicParsing | Out-Null
    } | Select-Object TotalMilliseconds
}
```

継続的な低遅延が必要なら`minReplicas`を1以上にすることを検討します。ただし常時実行コストが増えるため、要件と費用を確認してから変更します。

コスト要件で`minReplicas: 0`を維持する場合は、起動中ノイズと本障害を監視で分離します。

- 起動失敗は`ApplicationStartupFailed`（`startup.failure.severity=expected_startup_failure`）として1起動1件で集約する。
- 通知は`ApplicationReady`から`N`分（例: 5分）経過後も継続する`HttpRequestFailed`だけを対象にする。
- `BeanCreationException`/`DatabaseException`/`PSQLException`の起動時連鎖は即時ページング対象にしない。

### 5xx、起動失敗、再起動が発生する

Azureの推奨順序に従い、症状、Resource Health、ログ、メトリック、直前変更の順で確認します。

```powershell
$ApiApp = '<API_CONTAINER_APP_NAME>'
az containerapp logs show --name $ApiApp --resource-group SREagent-lab --type system --tail 100
az containerapp logs show --name $ApiApp --resource-group SREagent-lab --type console --tail 100
az containerapp revision list --name $ApiApp --resource-group SREagent-lab --output table
az monitor activity-log list --resource-group SREagent-lab --max-events 50 --output table
```

確認する代表例:

- イメージ取得、マネージドID、ACR RBAC
- Key Vault参照とシークレット
- PostgreSQL接続、DNS、Liquibase
- ターゲットポート`8081`とヘルス状態
- OOM、CPU・メモリ不足、レプリカ再起動
- 最新リビジョンの失敗とトラフィック割り当て

障害中に設定を連続変更せず、まず失敗リビジョンと正常リビジョンの差分を保存します。復旧を優先する場合は、確認済みの正常イメージタグとリビジョンへ戻す計画を立て、影響を確認してから実行します。

### PostgreSQLまたはLiquibaseで起動に失敗する

本番APIは`production`プロファイルでLiquibaseマイグレーションを実行します。コンソールログで最初のLiquibase例外と、その内側のPostgreSQL例外を確認します。

- PostgreSQL Flexible ServerとPrivate DNSの状態
- APIのVNet統合
- Key Vaultのシークレット参照とマネージドIDの権限
- 適用済みchange setと新しいSQLの互換性
- DBの容量、接続数、フェイルオーバー・復元履歴

StoreとAdminがHTTP 200でも、APIだけがタイムアウトし、Liquibaseの内側に`SocketTimeoutException: connect timed out`がある場合は、最初にFlexible Serverの状態を確認します。

```powershell
$PgName = '<POSTGRES_SERVER_NAME>'
$ApiApp = '<API_CONTAINER_APP_NAME>'

az postgres flexible-server show `
    --resource-group SREagent-lab `
    --name $PgName `
    --query state `
    --output tsv

az postgres flexible-server start `
    --resource-group SREagent-lab `
    --name $PgName `
    --only-show-errors

az containerapp revision restart `
    --resource-group SREagent-lab `
    --name $ApiApp `
    --revision '<LATEST_REVISION>' `
    --only-show-errors
```

Flexible Serverが`Ready`になってからAPIリビジョンを再起動し、`/api/wines`のHTTP 200、最新リビジョンの`Healthy`、Liquibaseエラーの解消を確認します。サーバーが停止した理由はアクティビティログと運用履歴で別途確認し、状態だけから自動停止や利用者操作と断定しません。

PostgreSQLはプライベート構成です。診断のために安易にパブリックアクセスを有効化せず、Azureコントロールプレーンと承認済みの一時VNet接続経路を使用します。適用済みchange setを書き換えず、新しい修正マイグレーションを追加します。

### 商品画像を登録または表示できない

商品画像は、ローカルではAPIプロセス内のメモリ、本番では非公開のAzure Blob Storageへ保存します。本番APIはユーザー割り当てマネージドIDでBlobへアクセスし、ストレージ接続文字列やアカウントキーは使用しません。

最初にHTTPステータスを確認します。

- `400 Bad Request`: 画像を読み取れない
- `404 Not Found`: 指定した画像が存在しない
- `413 Payload Too Large`: 5 MBを超えている
- `415 Unsupported Media Type`: JPEG、PNG、WebP以外、またはファイル署名が不正
- `500 Internal Server Error`: Blob接続、認証、ネットワークなどの本番依存関係

本番の`500`では、APIコンソールログとAzure構成を確認します。

```powershell
$ApiApp = '<API_CONTAINER_APP_NAME>'
az containerapp show --name $ApiApp --resource-group SREagent-lab --output json |
    ConvertFrom-Json |
    Select-Object -ExpandProperty properties |
    Select-Object -ExpandProperty template |
    Select-Object -ExpandProperty containers |
    Select-Object -ExpandProperty env |
    Where-Object { $_.name -in @('AZURE_STORAGE_BLOB_ENDPOINT', 'AZURE_STORAGE_BLOB_CONTAINER_NAME', 'AZURE_CLIENT_ID') } |
    Select-Object name, value, secretRef
```

確認点:

- `AZURE_STORAGE_BLOB_ENDPOINT`と`AZURE_STORAGE_BLOB_CONTAINER_NAME`が設定されている
- `AZURE_CLIENT_ID`がAPI用マネージドIDを指している
- API用IDにストレージスコープの`Storage Blob Data Contributor`がある
- Blobコンテナーが存在し、パブリックアクセスが無効である
- APIのVNetからBlob Private EndpointをPrivate DNSで解決できる
- 直前にストレージ、Private Endpoint、Private DNS、RBACを変更していない

上記コマンドは環境変数の値を表示します。現在の値はシークレットではありませんが、将来シークレットを追加した場合に備え、共有前に出力を確認します。ローカルの画像はAPI再起動で消えるのが現在の仕様です。本番の画像障害を回避する目的でBlobのパブリックアクセスを有効化しません。

## テストと検証

### Maven出力が大きく、失敗原因を見つけにくい

まず通常のテストを実行し、Surefireの要約を確認します。

```powershell
Set-Location backend
mvn test
Get-ChildItem target/surefire-reports/*.txt |
    Select-String -Pattern 'Tests run:|Failures:|Errors:'
```

詳細は対象スイートのXMLまたはTXTだけを確認します。大量の全ログを共有せず、最初の原因例外、失敗テスト名、期待値と実値を記録します。

2026年8月14日時点の最新実行は、24テスト、失敗0、エラー0、スキップ0です。この値は実行時点の記録であり、コード変更後は必ず`mvn test`を再実行します。

### 最低限のリリース確認

```powershell
Set-Location backend
mvn test

Set-Location ../frontend
npm run build:ops

Set-Location ../admin-frontend
npm run build

Set-Location ..
az bicep build --file infra/main.bicep
./infra/deploy.ps1 -ResourceGroupName SREagent-lab -ValidateOnly
./infra/deploy.ps1 -ResourceGroupName SREagent-lab -WhatIf
```

ローカルDockerを利用できる場合だけ、Store、Admin、APIのイメージビルドも実行します。Dockerを利用できない場合は、デプロイスクリプトのACR Linuxビルドを代替検証経路として使用します。

## エスカレーション時に添付する情報

- 発生日時とタイムゾーン
- 影響を受けたStore、管理画面、API、管理APIの範囲
- 再現手順、期待結果、実際の結果、HTTPステータス
- AzureサブスクリプションID、リソースグループ、リソース名
- Container Appのリビジョン名とイメージタグ
- デプロイ名、ACR Run ID、直前の変更
- Application Insightsの`operation_Id`
- 関連するログの時刻と最小限の抜粋
- 実施済みの確認と結果

次は添付しません。

- Application Insights接続文字列
- PostgreSQLパスワード
- Key Vaultシークレット
- AzureトークンやCookie
- 顧客名、メールアドレス、配送先住所
- 注文要求本文や画像バイナリ

## トラブル対応履歴

問題を調査または修正した場合は、解決・未解決を問わずこの節へ追記します。既存の診断手順も、得られた知見に合わせて更新します。秘密情報や個人情報は記録しません。

### 記録形式

```markdown
### YYYY-MM-DD: 事象名

- 症状・影響: 何が、どの環境・機能で失敗したか
- 原因・仮説: 確定した原因。未解決の場合は現時点の仮説
- 証跡: HTTPステータス、例外型、リビジョン、Run ID、確認したログなど
- 対応: 実施した修正または回避策
- 検証: 再実行したテスト、ビルド、要求とその結果
- 再発防止: 追加したテスト、監視、手順、設定上の対策
- 状態: 解決、暫定回避、未解決、環境上未検証のいずれか
```

### 2026-08-13: Application InsightsへAPIテレメトリが送信されない

- 症状・影響: Application Insightsリソースと接続文字列は存在したが、APIの要求、依存関係、ログ、業務メトリックを収集できなかった。
- 原因・仮説: JavaアプリケーションにApplication Insights JavaエージェントまたはSDKがなく、接続文字列だけが設定されていた。
- 証跡: Maven依存関係、Dockerfile、アプリケーションコードに計装がなく、Application InsightsでAPIテレメトリを確認できなかった。
- 対応: Javaエージェント3.7.8、SHA-256検証、Micrometer、構造化ログ、例外ログ、ライフサイクルログを追加した。
- 検証: バックエンドテスト、ACR Linuxイメージビルド、本番API要求、Application Insightsの`ApplicationReady`を確認した。
- 再発防止: エージェントのバージョンとSHA-256を同時管理し、接続文字列だけでは計装されないことと確認用KQLを本書へ記載した。
- 状態: 解決

### 2026-08-14: 停止中PostgreSQLによりAzure What-Ifが失敗する

- 症状・影響: Azure Validateは成功したが、サブスクリプションスコープのWhat-Ifが完了せず、デプロイ前の変更確認を実施できなかった。
- 原因・仮説: 既存PostgreSQL Flexible Serverが`Stopped`で、What-Ifのリソース状態予測がサーバー情報を参照できなかった。
- 証跡: `ServerStoppedError`と対象サーバーの`Stopped`状態を確認した。
- 対応: Flexible Serverを起動し、状態が`Ready`になった後にWhat-Ifを再実行した。
- 検証: What-Ifは9件作成、15件変更、削除0件として成功した。
- 再発防止: デプロイ前確認へ`ServerStoppedError`時の起動・状態確認手順を追加した。
- 状態: 解決

### 2026-08-17: 停止中PostgreSQLにより本番APIがタイムアウトする

- 症状・影響: StoreとAdminはHTTP 200だったが、APIルートと`GET /api/wines`がタイムアウトし、商品情報を取得できなかった。
- 原因・仮説: PostgreSQL Flexible Serverが`Stopped`でAPIがDBへ接続できなかった。サーバーが停止した契機は未確定。
- 証跡: APIリビジョン`azapidepgzxcukhrdm--0000009`のログでLiquibase初期化中の`PSQLException`と`SocketTimeoutException: connect timed out`を確認し、AzureコントロールプレーンでFlexible Serverの`Stopped`状態を確認した。
- 対応: Flexible Serverを起動して`Ready`を確認し、APIリビジョンを再起動した。
- 検証: APIリビジョンが`Healthy`かつ`Provisioned`となり、`GET /api/wines`はHTTP 200で6商品を返し、6商品すべてに説明文が存在した。
- 再発防止: APIだけがタイムアウトする場合のFlexible Server状態確認、起動、APIリビジョン再起動、復旧確認手順を本書へ追加した。停止契機はアクティビティログと運用履歴で確認する。
- 状態: 解決

### 2026-08-17: APIコールドスタート時のPostgreSQL接続失敗がアラートノイズ化する

- 症状・影響: コールドスタートごとに`PSQLException`→`DatabaseException`→`BeanCreationException`が記録され、同種の起動失敗が短時間に複数件通知される。
- 原因・仮説: `minReplicas: 0`環境で起動直後のPrivate DNS解決/ネットワーク経路確立が間に合わず、Liquibase初期化で一時的にDB接続失敗する。
- 証跡: 24時間で同一例外連鎖が周期的に発生し、後続リトライ成功後は`GET /api/wines`がHTTP 200で復旧した。
- 対応: 起動失敗を`ApplicationStartupFailed`として1起動1件で記録し、既知連鎖は`expected_startup_failure`としてWARN集約へ変更した。監視クエリは`ApplicationReady`後`N`分経過しても継続する`HttpRequestFailed`のみ通知する運用へ更新した。
- 検証: バックエンド単体テストを追加して既知例外連鎖の判定を確認し、監視用KQLを`docs/observability.md`へ反映した。
- 再発防止: 起動中例外と起動後障害のアラート条件を分離し、既知の起動時接続失敗は集約指標として扱う。
- 状態: 解決

### 2026-08-14: AdminのACRビルドが接続文字列とlockfileにより失敗する

- 症状・影響: ブートストラップデプロイ後、AdminイメージのACRビルドが失敗し、最終アプリデプロイへ進まなかった。
- 原因・仮説: Application Insights接続文字列のセミコロンがACR内部シェルで分割された。修正後はlockfileが開発環境固有のパッケージURLを固定し、ACRからの取得を拒否された。
- 証跡: ACR Runで`docker build requires exactly 1 argument`と`EALLOWREMOTE`を確認した。
- 対応: 接続文字列をBase64化して渡し、コンテナー内で復号した。Adminへ公開npmレジストリ設定と`.dockerignore`を追加し、Dockerfileを`package.json`からの依存解決へ変更した。
- 検証: Adminを含む3件のACRビルド、最終ARMデプロイ、3リビジョンの`Healthy`、AdminのHTTP 200を確認した。
- 再発防止: ACRビルド確認項目へシェル特殊文字、lockfileの取得元、ビルドコンテキスト除外を追加した。
- 状態: 解決

### 2026-08-13: Docker Desktop停止によりローカルイメージを検証できない

- 症状・影響: ローカルでStoreとAPIのLinuxコンテナーイメージをビルドできなかった。
- 原因・仮説: Docker DesktopのLinuxエンジンが起動していなかった。
- 証跡: DockerクライアントからServer情報を取得できなかった。
- 対応: Azure Container RegistryのLinuxビルドを代替検証経路として使用した。
- 検証: ACR上でStoreとAPIのイメージビルドが成功し、そのイメージをAzureへデプロイしてHTTP応答を確認した。
- 再発防止: ローカルでは`docker version`と`docker info`を先に確認し、利用できない場合のACRビルド手順を本書へ記載した。
- 状態: 暫定回避

### 2026-08-13: npmレジストリ応答待ちで販売サイトのローカルビルドが完了しない

- 症状・影響: npm依存関係の取得が進まず、販売サイトのローカルビルドを完了できなかった。
- 原因・仮説: npmレジストリまたはネットワークから応答を得られなかった。依存関係やソースコードの失敗とは確定していない。
- 証跡: npm依存関係インストールがレジストリ応答待ちとなった。
- 対応: StoreのACRビルドを使用して依存関係取得と本番成果物を検証した。
- 検証: ACRビルド、Azureデプロイ、本番StoreのHTTP 200を確認した。
- 再発防止: `npm config get registry`と`npm ping`による事前確認、原因確定前にロックファイルを削除しない方針を本書へ記載した。
- 状態: 暫定回避

### 2026-08-13: Azureリソース検索結果とWindows上のCLIクエリが安定しない

- 症状・影響: Resource Graphでは`SREagent-lab`のリソースを取得できず、特殊文字を含むJMESPathクエリはWindowsのコマンド解析で失敗した。
- 原因・仮説: Resource Graphと現在のAzure CLIコンテキストまたは反映状態の差、およびシェルによるJMESPath特殊文字の解釈が影響した。
- 証跡: Resource Graphの空結果に対し、リソースグループを指定したAzure CLIではリソースを確認できた。複雑な`--query`は解析エラーになった。
- 対応: サブスクリプションを明示確認し、`az resource list --output json`の結果をPowerShellの`ConvertFrom-Json`で解析した。
- 検証: Store、API、Azure SRE Agentなど対象リソースの存在と状態を取得できた。
- 再発防止: Resource Graphの空結果だけで不存在と判断せず、リソースグループ指定のAzure Resource Manager結果と照合する手順を本書へ記載した。
- 状態: 解決

## 関連文書

- [システム仕様](system-overview.md)
- [オブザーバビリティ設計](observability.md)
- [Azure本番デプロイ](azure-deployment.md)
