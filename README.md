# SRE Agent Wine Demo

このワークスペースには、Spring Boot APIを共有する購入者向けVue販売サイトと管理者ダッシュボードが含まれています。本番環境の商品・注文・在庫・発注はAzure Database for PostgreSQLへ、商品画像はAzure Blob Storageへ永続化され、Azure SRE Agentが読み取り専用で運用調査を行います。

## ドキュメント

- [システム仕様](docs/system-overview.md): アーキテクチャ、購入・管理フロー、API、データ保持
- [Azure本番デプロイ](docs/azure-deployment.md): Bicep、デプロイ、RBAC、Private Endpoint、運用制約
- [オブザーバビリティ設計](docs/observability.md): Application Insights、Log Analytics、ログ、メトリック、KQL
- [トラブルシューティング](docs/troubleshooting.md): ローカル・Azureの障害切り分けとリリース確認

## サイト

| サイト | ローカルURL | 本番URL | API | 用途 |
| --- | --- | --- | --- | --- |
| Maison Vigne Store | <http://localhost:3000> | <https://azstodepgzxcukhrdm.livelyfield-29cce79e.japaneast.azurecontainerapps.io> | `GET /api/wines`、`POST /api/orders` | 購入者向け販売サイト、カート、購入手続き |
| Maison Vigne Admin | <http://localhost:3001> | 未デプロイ | `/api/admin/dashboard`、`/api/admin/orders`、`/api/admin/inventory`、`/api/admin/purchase-orders` | サマリー、注文ステータス、在庫、発注・受取の管理 |

既存の`ops`スクリプトと成果物名は、デプロイ互換性のため維持しています。
Operation PulseとデモインシデントAPIは廃止済みで、アプリケーションとAzureデプロイには含まれません。

## 構成要素

- バックエンド: Spring Boot 2.7、Java 8、Maven
- フロントエンド: Vue 3、Viteによる販売サイトと管理画面
- データ: ローカル開発ではインメモリH2とプロセス内画像ストレージ、AzureではPostgreSQL 17と非公開Blob Storage
- インフラ: Azure Container Apps、Azure Container Registry、PostgreSQL、Blob Storage、マネージドID、Azure Monitor、Azure SRE Agent

## ローカル実行

APIを起動します。

```powershell
Set-Location backend
mvn spring-boot:run
```

別のターミナルで販売サイトを起動します。

```powershell
Set-Location frontend
npm install
npm run dev:ops
```

管理者ダッシュボードは独立したターミナルで起動します。

```powershell
Set-Location admin-frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

既定のAPIエンドポイントは`http://localhost:8081`です。管理画面では`admin-frontend/.env.example`を`.env.local`へコピーしてAPI URLを変更できます。ブラウザテレメトリを送信する場合は、同じファイルの`VITE_APPLICATIONINSIGHTS_CONNECTION_STRING`にApplication Insights接続文字列を設定します。未設定時はブラウザテレメトリだけが無効になります。

バックエンドは既定でローカル販売サイトのオリジンからの要求を許可します。デプロイ環境では次のように上書きします。

```powershell
$env:CORS_ALLOWED_ORIGINS = 'https://store.example.com'
mvn spring-boot:run
```

## ビルド

販売サイトをビルドします。

```powershell
Set-Location frontend
npm run build:ops
```

成果物は`frontend/dist/ops`へ出力されます。

管理者ダッシュボードは独立してビルドします。

```powershell
Set-Location admin-frontend
npm run build
```

成果物は`admin-frontend/dist`へ出力されます。管理画面では注文ステータスと在庫数を更新し、商品登録、ワインの発注・受取を行えます。納品予定日は土日を除いた5営業日後です。受取処理は発注レコードを削除し、同一トランザクションで対象商品の在庫を発注数だけ増加させます。Entra ID認証はまだ実装されていないため、本番デプロイ前に`/api/admin/**`をOAuth 2.0 Resource Serverとして保護してください。

バックエンドテストを実行します。

```powershell
Set-Location backend
mvn test
```

2026年8月14日時点の最新実行では、24テストが成功し、失敗・エラー・スキップはありません。

接続先APIのURLが確定した後、コンテナーイメージをビルドします。

```powershell
docker build frontend --build-arg SITE=ops --build-arg VITE_API_BASE_URL=https://API_HOST -t maison-vigne-ops:local
docker build admin-frontend --build-arg VITE_API_BASE_URL=https://API_HOST --build-arg VITE_APPLICATIONINSIGHTS_CONNECTION_STRING='APPLICATION_INSIGHTS_CONNECTION_STRING' -t maison-vigne-admin:local
docker build backend -t wine-api:local
```

## Azureインフラ

`infra/main.bicep`は、Japan Eastの本番環境を構成するサブスクリプションスコープのオーケストレーターです。リソースグループスコープのモジュールでは次を定義します。

- VNet統合されたStoreとAPIのContainer Apps。管理画面は現在のIaCに含まれない
- Liquibaseでバージョン管理されたマイグレーションを使用するプライベートPostgreSQL Flexible Server
- Private Endpointで接続し、マネージドIDで読み書きする非公開Azure Blob Storage
- Key Vault、およびイメージ取得用とAPIシークレット参照用の個別マネージドID
- Basic ACR、Log Analytics、ワークスペースベースのApplication Insights
- 専用Azure Monitor Workspaceとワークロードへの読み取り専用アクセスを持つAzure SRE Agent PaaS

APIコンテナーはApplication Insights Javaエージェント3.7.8を使用し、要求、JDBC依存関係、例外、Logbackログ、JVMテレメトリを自動収集します。注文処理と管理操作では、顧客名、メールアドレス、配送先住所を含まない構造化ログとMicrometer業務メトリックも送信します。管理画面はApplication Insights Browser SDKでページ表示、API依存関係、JavaScript例外、API失敗、更新成功を送信できます。コンテナーの標準出力と標準エラーは、引き続きLog Analyticsへ送信されます。

デプロイスクリプトは、ブートストラップ用エンドポイントの作成、ACRでの変更不可能なイメージのビルド、最終リビジョンの適用を行います。シークレットは実行時に生成され、コミット済みのパラメーター例には保存されません。

デプロイ前にテンプレートを検証します。

```powershell
az bicep build --file infra/main.bicep
./infra/deploy.ps1 -ValidateOnly
```

デプロイの前提条件、運用、低コスト構成で許容する可用性上のトレードオフについては、[Azure本番デプロイ](docs/azure-deployment.md)を参照してください。テレメトリの送信先、収集データ、プライバシー上の制約、確認用クエリについては、[オブザーバビリティ設計](docs/observability.md)を参照してください。

## 現在のデプロイ状態

リソースグループ`SREagent-lab`の本番デプロイは、2026年8月13日に次の内容を確認済みです。

- StoreはHTTP 200を返しました。
- `GET /api/wines`はHTTP 200と6商品を返しました。
- 廃止済みの`GET /api/demo/incidents`エンドポイントはHTTP 404を返しました。
- リソースグループ内のContainer AppsはStoreとAPIの2件のみです。
- 管理画面のDockerイメージとContainer Appは未デプロイです。管理APIを含む最新バックエンドコードの本番反映は、この日付の検証対象外です。
- Azure SRE Agent `azsredepgzxcukhrdm`はAustralia Eastで維持されています。
- Operation PulseのContainer App、Entraアプリ登録、ACRリポジトリは削除済みです。
