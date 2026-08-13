# SRE Agent Wine Demo

このワークスペースには、Spring Boot APIをバックエンドとする購入者向けVue販売サイトが含まれています。本番環境の注文と在庫はAzure Database for PostgreSQLへ永続化され、Azure SRE Agentが読み取り専用で運用調査を行います。

## ドキュメント

アーキテクチャ、購入フロー、API仕様、データ保持、ローカル実行、Azureインフラ、検証結果、本番環境の制約を含むシステム仕様全体は、[システム仕様](docs/system-overview.md)を参照してください。

## サイト

| サイト | ローカルURL | 本番URL | API | 用途 |
| --- | --- | --- | --- | --- |
| Maison Vigne Store | <http://localhost:3000> | <https://azstodepgzxcukhrdm.livelyfield-29cce79e.japaneast.azurecontainerapps.io> | `GET /api/wines`、`POST /api/orders` | 購入者向け販売サイト、カート、購入手続き |

既存の`ops`スクリプトと成果物名は、デプロイ互換性のため維持しています。
Operation PulseとデモインシデントAPIは廃止済みで、アプリケーションとAzureデプロイには含まれません。

## 構成要素

- バックエンド: Spring Boot 2.7、Java 8、Maven
- フロントエンド: Vue 3、Vite
- データ: ローカル開発ではインメモリH2、AzureではPostgreSQL 17
- インフラ: Azure Container Apps、Azure Container Registry、PostgreSQL、マネージドID、Azure Monitor、Azure SRE Agent

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

既定のAPIエンドポイントは`http://localhost:8081`です。別のエンドポイントを使用する場合は、`frontend/.env.example`を`.env.ops.local`など適切なViteモードファイルへコピーし、`VITE_API_BASE_URL`を設定します。

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

接続先APIのURLが確定した後、コンテナーイメージをビルドします。

```powershell
docker build frontend --build-arg SITE=ops --build-arg VITE_API_BASE_URL=https://API_HOST -t maison-vigne-ops:local
docker build backend -t wine-api:local
```

## Azureインフラ

`infra/main.bicep`は、Japan Eastの本番環境を構成するサブスクリプションスコープのオーケストレーターです。リソースグループスコープのモジュールでは次を定義します。

- VNet統合されたStoreとAPIのContainer Apps
- Liquibaseでバージョン管理されたマイグレーションを使用するプライベートPostgreSQL Flexible Server
- Key Vault、およびイメージ取得用とAPIシークレット参照用の個別マネージドID
- Basic ACR、Log Analytics、ワークスペースベースのApplication Insights
- 専用Azure Monitor Workspaceとワークロードへの読み取り専用アクセスを持つAzure SRE Agent PaaS

APIコンテナーはApplication Insights Javaエージェント3.7.8を使用し、要求、JDBC依存関係、例外、Logbackログ、JVMテレメトリを自動収集します。注文処理では、顧客名、メールアドレス、配送先住所を含まないMicrometer業務メトリックも送信します。コンテナーの標準出力と標準エラーは、引き続きLog Analyticsへ送信されます。

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
- Azure SRE Agent `azsredepgzxcukhrdm`はAustralia Eastで維持されています。
- Operation PulseのContainer App、Entraアプリ登録、ACRリポジトリは削除済みです。
