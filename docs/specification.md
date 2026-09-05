# thinreports-java 仕様書

---

## 1. 概要

### 1.1 目的
Ruby 用帳票ソリューション thinreports をベースに、オーバーレイ形式のテンプレート（`.tlf`）から
PDF を生成する Java ライブラリを開発する。Ruby版がサポートするレポート形式・文字装飾をすべて Java で再現する。

### 1.2 スコープ
- 対象: `.tlf`（Thinreports レイアウトファイル。実体は JSON）を読み込み、データ（キー/値マップ）を
  差し込んで PDF を生成するライブラリ本体。
- 対象外（初期）: レイアウトエディタ、リスト（section/table）繰り返しの動的レイアウト（Ruby版では
  `list` に相当。※後述の課題で扱う）。

### 1.3 成果物・座標
| 項目 | 内容 |
|------|------|
| groupId | `com.github.naofum` |
| artifactId | `thinreports-java` |
| version | `1.0.0` |
| Java | 21 |
| ライセンス | Apache License 2.0 |

### 1.4 前提技術
| 項目 | 採用 | 備考 |
|------|------|------|
| 言語 | Java 21 | |
| PDF エンジン | **Apache PDFBox 3.0.x（生 API を直接利用）** | 下記 4.1 参照。外部レイアウトエンジンには依存しない |
| JSON | org.json | `.tlf` パース用 |
| テスト | JUnit 5 | |
| 配布 | GitHub Packages (Maven) | |
| バーコード/QR/チャート | **本体に含めず examples に留める** | |

> **レイアウト方針**: thinreports の tlf は各 item が `x/y/width/height` を持つ**絶対座標オーバーレイ**である。
> フロー型レイアウトエンジン（pdfbox-layout / ph-pdf-layout 等）は自動積み上げが本質で本用途と噛み合わず、
> 絶対配置を無理に行うと座標制御が複雑化する。よって **PDFBox の生 API（`PDPageContentStream`）で
> 指定座標に直接描画する方式**を採用する。
> テキストの折返し・整列・装飾 markup 処理は自前実装する（4.3 参照）。

---

## 2. 用語

| 用語 | 意味 |
|------|------|
| tlf | Thinreports レイアウトファイル。JSON 形式。`report` / `items` / `settings` を持つ |
| item | tlf 上の描画要素（text, text-block, image, image-block, rect, ellipse, line） |
| オーバーレイ | 固定レイアウト上にデータ値を差し込んで描画する方式 |
| shape ID | item の `id`。データマップのキーとして値差し込みに使う |

---

## 3. 機能要件

### 3.1 ドキュメント生成 API
プロトタイプの `TRGenerator` を基準とする。

- `newDocument(tlffile, outfile, map)` — tlf ファイル＋値マップから新規ドキュメント生成
- `newDocument(json, outfile, map)` — tlf JSON から生成
- `newDocument(..., settings)` — メタ情報/セキュリティ設定を外部指定
- `addPage(tlffile|json, map)` — 既存ドキュメントへページ追加（複数レイアウト対応）
- `save(filename)` — PDF 保存
- `getDocument()` / `getSettings()`

### 3.2 用紙・レイアウト
- 用紙サイズ: A0～A6、B0～B6、letter、legal（`report.paper-type`）**
- 向き: portrait / landscape（`report.orientation`）
- マージン: `report.margin` = [top, right, bottom, left]
- 座標変換: tlf は左上原点、PDF は左下原点。`y' = pageHeight - y` で変換

### 3.3 描画要素
| type | 対応内容 |
|------|----------|
| text / text-block | 複数行テキスト、フォント、装飾、整列、書式変換 |
| image / image-block | Base64埋め込み画像 / マップ経由 BufferedImage、縮尺・配置 |
| rect | 矩形、角丸(border-radius)、枠線、塗り |
| ellipse | 楕円、枠線、塗り |
| line | 直線、枠線スタイル |

### 3.4 テキスト装飾（文字装飾）
Ruby版と同等を目標とする: 
- font-family: Helvetica / Times / Courier / IPAGothic / IPAMincho / IPAPGothic / IPAPMincho
- font-size, color
- font-style: bold / italic / underline / linethrough
- text-align: left / center / right / justify
- **未対応/要確認（課題）**: vertical-align（読み取りはするが未反映）、line-height / line-height-ratio、letter-spacing

### 3.5 値の書式変換（format）
- number: delimiter（桁区切り）、precision（小数桁）、base（`{value}` 差し込み）
- padding: char / length / direction
- datetime: `%Y %m %d` → Java format 変換
- **要拡充（課題）**: 時分秒（%H %M %S 等）、boolean 書式、Ruby版の全書式網羅

### 3.6 セキュリティ / メタ情報
- title / creator / author / producer
- security_settings: user_password / owner_password / permissions（print_document, modify_contents, copy_contents）
- 暗号化キー長 128bit

### 3.7 フォント
- IPA フォント（TTF）を外部ファイルとして参照（`Settings` にパス保持）
- 埋め込みフォント対応（EUDC 外字テスト有）

---

## 4. アーキテクチャ方針

### 4.1 レイアウト戦略（生 API 中心）
- PDFBox の `PDDocument` / `PDPage` / `PDPageContentStream` を直接操作する。
- 座標変換: tlf は左上原点(pt)、PDF は左下原点。`pdfY = pageHeight - tlfY`（要素高さ分の補正を要素種別ごとに考慮）。
- 各要素はページ上の絶対座標へ描画。フロー配置・自動改ページは行わない（明細の list を除く。7章課題）。

### 4.2 パッケージ構成
```
com.github.naofum.thinreports
├── TRGenerator        … 生成オーケストレーション（公開API・プロトタイプ互換の入口）
├── Settings           … フォントパス・padding・比率等の設定
├── model/             … tlf を型付けする DTO（report, item, style, format）
├── layout/            … 座標系変換・ページ生成（PageContext, CoordinateMapper）
├── render/            … type 別の描画（PDPageContentStream への直接描画）
│                         TextRenderer / ImageRenderer / RectRenderer /
│                         EllipseRenderer / LineRenderer
├── text/              … テキスト折返し・整列・markup 装飾処理（自前実装）
│                         LineBreaker / TextDecorator / MarkupParser
├── font/              … フォント解決・埋め込み（Standard14 + IPA/TTF）
└── format/            … number / padding / datetime 書式変換
```

### 4.3 自前実装が必要な領域（旧 pdfbox-layout が担っていた機能）
| 機能 | 旧依存 | 生 API での対応 |
|------|--------|-----------------|
| テキスト折返し | Paragraph.setMaxWidth | フォント幅計測(`PDFont.getStringWidth`)で幅内改行を算出 |
| 整列 (left/center/right/justify) | Alignment | 行ごとに開始 x を計算、justify は語間スペース調整 |
| 縦整列 (top/middle/bottom) | （未対応だった） | ブロック高と行高から y オフセット算出 |
| 装飾 markup (`{color:}` `*bold*` `__underline__` 等) | Paragraph.addMarkup | MarkupParser で解析し、色/太字/下線/取消線を描画時に反映 |
| 枠線スタイル (dashed/dotted) | Stroke/DashPattern | `setLineDashPattern` で再現 |
| 図形 (rect/roundrect/ellipse/line) | Shape | `addRect`/ベジェ曲線/`moveTo-lineTo` で描画 |

> markup 記法は Ruby版 tlf 互換を維持する（プロトタイプの `{color:#..}` `*..*` `_.._` `__..__` `__{0.25:}..__`）。
> 実装の参考として ralfstuckert/pdfbox-layout（MIT）および ph-pdf-layout-richtext のパーサ設計を参照してよい
> （コード流用ではなくアルゴリズム参照）。

---

## 5. 非機能要件

- **テスト**: JUnit 5。要素種別・書式・list/page-number/markup/group-rows のユニット＋統合テスト、
  および視覚回帰テスト（PDF→PNG ラスタライズ＋ゴールデン比較）を整備。PDF 出力は PDFTextStripper で
  内容検証、レイアウトはゴールデン画像で回帰検証する。
- **互換性**: Ruby版の tlf をそのまま読み込めること。
- **配布**: GitHub Packages への Maven publish（`com.github.naofum:thinreports-java:1.0.0`）。
  タグ push で `publish.yml` が自動デプロイ。手順は `docs/publishing.md` を参照。
- **ライセンス**: Apache License 2.0（プロトタイプ踏襲）。
- **文字コード**: UTF-8。

---

## 6. Ruby版との差分（対応要否一覧）

| 機能 | Ruby版 | プロトタイプ | 本プロジェクト方針 |
|------|--------|--------------|--------------------|
| 基本図形 (text/image/rect/ellipse/line) | ○ | ○ | 完成・検証 |
| list（明細の繰り返し/改ページ） | ○ | ✕ | **実装済み（header/detail/footer + auto-page-break）** |
| stack view / 動的レイアウト | ○ | ✕ | 要調査 |
| 全用紙サイズ | ○ | A系のみ | 要拡充 |
| vertical-align / line-height / letter-spacing | ○ | 一部✕ | 要実装 |
| 全書式(datetime時刻/boolean) | ○ | 一部 | 要拡充 |
| バーコード / QR | ○(拡張) | test/example で例示 | **本体外・examples に留める（今後検討）** |
| チャート | 例示 | test/example で例示 | **本体外・examples に留める（今後検討）** |
| テキスト折返し/整列/装飾 | ○(pdfbox-layout) | pdfbox-layout 依存 | **生 API で自前実装（4.3）** |

---

## 7. 受け入れ基準（抜粋）

1. Ruby版 tlf サンプル一式が例外なく PDF 生成できる。
2. text の装飾（bold/italic/underline/linethrough/color/align）が視覚的に一致する。
3. number/padding/datetime 書式が Ruby版と同一出力になる。
4. 複数レイアウトの `addPage` で複数ページ PDF が生成できる。
5. セキュリティ設定付き PDF が正しく保護される。
6. すべての要素種別にユニットテストが存在し green。

---

## 8. 進め方（フェーズ）

1. **基盤整備** — 新規 Maven プロジェクト（`com.github.naofum:thinreports-java:1.0.0` / Java 21）。
   PDFBox 3.0.x と org.json のみを依存に、JUnit 5、GitHub Packages publish 設定、CI。
   examples モジュール（barcode4j/zxing/jfreechart）は別 pom で分離。
2. **描画コア（生 API）** — `layout/`（座標変換・ページ生成）と `render/` の rect/ellipse/line/image を
   `PDPageContentStream` で実装。まず装飾なしの静的描画を tlf サンプルで一致させる。
3. **テキスト処理（自前）** — `text/` に折返し・整列・markup 装飾（4.3）を実装。
   Ruby版 tlf の text 装飾（bold/italic/underline/linethrough/color/align）を視覚一致させる。
4. **書式・フォント・セキュリティ** — `format/`（number/padding/datetime 完全対応）、
   `font/`（IPA/TTF 埋め込み）、メタ情報・暗号化。
5. **Ruby版差分の実装** — list（明細繰り返し・改ページ）、全用紙サイズ、vertical-align/line-height/letter-spacing。
6. **検証・公開** — Ruby版 tlf 一式で PDF 生成、視覚/書式一致確認、publish。

## 9. 実装状況

- **list**: header / detail（繰り返し）/ page-footer / footer + auto-page-break を実装済み。
  - `addRow(row)` / `addRow(listId, row)` で明細行を追加（Ruby版 `list.add_row` 相当）。
  - `setPageFooterValues(...)` は各ページ末の page-footer に、`setFooterValues(...)` は最終ページの
    footer に値を差し込む（Ruby版 `on_page_footer_insert` / `on_footer_insert` の簡易版）。
  - **ページ別 page-footer コールバック**: `setPageFooterCallback((pageNumber, pageRows) -> values)` で
    ページごとに異なる page-footer 値を返せる（Ruby版 `on_page_footer_insert` のページ集計相当）。1 始まりの
    ページ番号とそのページに配置された明細行リストを受け取り、返した値は静的値（`setPageFooterValues`）に
    上書きマージされる。ページ小計などのページ別集計に対応。
  - **footer 集計コールバック**: `setFooterCallback(allRows -> values)` で全明細行から総合計等を集計して
    footer 値を返せる（Ruby版 `on_footer_insert` 相当）。返した値は静的値（`setFooterValues`）に上書き
    マージされ、最終ページの footer 描画直前に一度だけ呼ばれる。
  - 描画は `save()` 時に一括実行（行データ確定後に確定描画）。
  - 検証: `ListTest`（basic_list 60行の複数ページ分割、advanced_list の page-footer/footer 値が
    PDF テキストに出力されること、ページ別コールバックがページ1/2 に異なる小計値を出力すること、
    footer 集計コールバックが全60行の合計値 `TOTAL-1770-n60` を出力することを確認）。
- **page-number**: `{page}`（現在ページ）/ `{total}`（総ページ数）のプレースホルダに対応。全ページ
  確定後に2パス目で各ページへ描画するため、list の改ページを含めた正しい総数・ページ番号を反映する。
  - 検証: `PageNumberTest`（3ページ各々に `Page N of 3` が出力されることを確認）。
- **markup インライン装飾**: text/text-block の値内の記法に対応（`text/MarkupParser`）。
  - `{color:#rrggbb}`（以降の色変更）、`*bold*`、`_italic_`、`__underline__`、`__{n:}line-through__`。
  - 行を `TextRun` セグメントに分解し、`TextRenderer` がセグメントごとに font/color/下線/取消線を切替えて
    x を進めながら描画。要素の font-style（常時 underline/linethrough）はマージされる。
  - **折返し対応**: 折返しは `TextRun` 列を「視覚行」に分割する方式に統一され、markup を含む行も
    要素幅で複数行に折り返される。折返しは単語境界（空白後・CJK 文字間）を優先し、単独で幅を超える
    トークンのみ文字単位に分割する。plain テキストも同じ経路を通る。
  - 検証: `MarkupParserTest`（各記法の分解）と `MarkupRenderTest`（マーカー除去に加え、幅超過の markup
    文字列が複数視覚行に折り返されても全トークンが出力されることを確認）。
- **group-rows（行単位の要素制御）**: 明細行マップの予約キー `ListRenderer.HIDDEN_IDS_KEY`
  （`"__hidden__"`, 値は `Set<String>`）に item id を入れると、その行では該当要素を描画しない。
  グループ見出しを先頭行のみ表示する等のグループ表現に対応。
  - `format.base`（`"... {value}"`）は `type` が空でも適用されるよう修正。
  - 検証: `GroupRowsTest`（グループ先頭行のみ見出しが出力され、継続行で非表示になることを確認）。
- **視覚一致検証（ゴールデン回帰）**: `test/.../visual/VisualCompare` が PDFBox `PDFRenderer` で
  ページを PNG ラスタライズし、ピクセル差分率（チャンネル別 tolerance 付き）を算出する。
  - `TextBlockVisualTest` が `src/test/resources/golden/*.png` と比較し差分率が閾値（1%）以内かを検証。
    `-DupdateGolden=true` またはゴールデン未存在時はベースラインを生成（初回でもビルドは壊れない）。
  - 検証: 差分ロジックの単体テスト（`VisualCompareTest`）＋ text_block のゴールデン比較が green。
- **examples モジュール分離（barcode / QR / chart）**: 本体（core）とは別の Maven モジュール
  `examples/`（`com.github.naofum:thinreports-java-examples`）として分離。本体を通常依存で参照し、
  画像生成ライブラリ（barcode4j / zxing / jfreechart）は examples 側のみに置く（本体は無依存）。
  - `BarcodeGenerators`（ean13/ean8/qr）・`ChartGenerators`（bar/line/pie）が `BufferedImage` を生成し、
    本体の image-block へデータマップ経由で差し込む方式。
  - 検証: `BarcodeExampleTest` / `ChartExampleTest`（画像生成→PDF 化が 1 ページで成功）。
    利用手順は本体を先に `mvn install` してから examples を `mvn test`。
- **GitHub Packages への publish 整備**: 本体 pom に `scm`・`distributionManagement`
  （repository / snapshotRepository）を設定し、`maven-source-plugin`（3.3.0）と
  `maven-javadoc-plugin`（3.6.2）で `-sources.jar` / `-javadoc.jar` を自動添付。
  - `.github/workflows/ci.yml`: push/PR で本体 `verify` ＋ examples テスト。
  - `.github/workflows/publish.yml`: バージョンタグ（`v*`）push または手動実行で `GITHUB_TOKEN`
    （`packages: write`）を用いて `mvn deploy`。
  - 手順は `docs/publishing.md`（自動 publish・ローカル手動 publish・消費側設定）に記載。
  - 検証: `mvn package` で 3 種 jar（本体 / sources / javadoc）が生成されることを確認。
- **用紙サイズの網羅**: A0–A6・Letter・Legal に加え、ISO 216 B 系（B0–B6）を mm→pt 換算で対応
  （`layout/PageContext`）。paper-type は大小文字非依存、未知値は A4 にフォールバック。landscape は
  幅高さを入れ替え。
  - 検証: `PageContextTest`（B4/B5 寸法、landscape 反転、大小文字非依存、未知フォールバック、座標変換）。
- **datetime 書式の拡充**: Ruby strftime → `SimpleDateFormat` 変換を単一走査方式に刷新
  （`format/ValueFormatter#toJavaDatePattern`）。`%Y %y %m %B %b/%h %d %e %j %A %a %H %I %M %S %L %p %Z %z`
  に対応し、`%%` エスケープとリテラル文字（英字・引用符）の自動クオートを処理。曜日・月名は決定的出力の
  ため `Locale.ENGLISH` を明示。
  - 検証: `ValueFormatterTest`（曜日/月名/12時間/リテラル引用/`%%`エスケープ/英語ロケール曜日）。

## 10. 課題・リスク

- **集計はコールバック方式**。page-footer は `setPageFooterCallback(...)`（ページ別）、footer は
  `setFooterCallback(...)`（全行集計）で自動集計に対応済み（上記 実装状況参照）。行走査に完全連動した
  Ruby版イベント（`on_page_finalize` 等の逐次フック）ではなく、確定後の行リストを渡す方式である点が差異。
- **テキスト処理の自前実装コスト**。折返し・justify・縦整列・markup は旧 pdfbox-layout が担っていた領域。
  日本語（IPA フォント）の幅計測・改行位置の正確さが品質の鍵。
- **書式の残細目**（`%U`/`%W` 週番号、`%G`/`%V` ISO 週など strftime の低頻度ディレクティブ）は未対応。
- **Ruby版出力との厳密比較は未実施**。エンジン差（PDFBox vs Prawn）によりピクセル完全一致は困難なため、
  現状は自己ゴールデン回帰で描画崩れを検知（下記 実装状況の「視覚一致検証」参照）。Ruby版 PDF との
  許容差分比較（緩い閾値）は今後の拡張として `VisualCompare` に追加可能。
