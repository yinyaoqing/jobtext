# jobtext

把職缺匯出的 CSV 解析成 Markdown 報表與摘要 CSV：從自由文字中抽出薪資、遠端政策、時區與技術關鍵字，並對每一筆不合格的輸入回報行號與原因。

純 Java 21 標準函式庫實作，執行時沒有任何第三方依賴；測試使用 JUnit 5。

> **English summary.** A dependency-free Java 21 CLI that reads job-posting CSV exports and writes a Markdown report plus a one-row-per-posting CSV. It parses salaries in Taiwanese and English formats, remote-work wording, UTC offsets and tech keywords from free text, validates every row, and reports skipped rows with line numbers. 89 JUnit tests; CI runs the tool on the example data and diffs the output.

## 快速開始

```bash
./mvnw verify                      # 編譯、跑測試、打包（Windows 用 mvnw.cmd）
java -jar target/jobtext-0.1.0.jar --in examples/postings.csv --out report.md --csv summary.csv
```

範例輸入：[examples/postings.csv](examples/postings.csv)，對應輸出：[examples/report.md](examples/report.md)、[examples/summary.csv](examples/summary.csv)。範例中的公司都是虛構的。

## 輸入格式

CSV 第一列為欄位名稱，不分大小寫、順序不拘：

| 欄位 | 必填 | 說明 |
|---|---|---|
| `id` | ✓ | 英數字與 `. _ -`，最多 64 字元，不可重複 |
| `title` | ✓ | 職稱 |
| `company` | ✓ | 公司 |
| `description` | ✓ | 職缺內容，可為 HTML，或被 ATS 再跳脫一次的 HTML（`&lt;p&gt;…`） |
| `location` |  | 地點，也用來找時區 |
| `salary` |  | 薪資文字；沒有時改從 `description` 找 |
| `remote` |  | 遠端說明；會和 `description` 一起判斷 |

## 選項

```
--in <file>          輸入 CSV
--out <file>         輸出 Markdown 報表
--csv <file>         另外輸出摘要 CSV
--encoding <name>    輸入編碼，預設 UTF-8；舊版 Excel 存的檔案可用 Big5
--max-bytes <n>      輸入大小上限，預設 10 MB
--strict             有任何列被略過就以 exit code 1 結束
```

Exit code：`0` 成功、`1` 搭配 `--strict` 時有列被略過、`2` 參數錯誤、`3` 輸入或輸出錯誤。

## 做了哪些文本處理

| 模組 | 做法 |
|---|---|
| [`CsvReader`](src/main/java/io/github/yinyaoqing/jobtext/csv/CsvReader.java) | 自己寫的 RFC 4180 狀態機：引號內的逗號、換行與 `""`、CRLF / LF / CR、BOM；每列記住起始行號，引號跨行也能回報正確位置 |
| [`CsvWriter`](src/main/java/io/github/yinyaoqing/jobtext/csv/CsvWriter.java) | 必要時才加引號；以 `= + - @` 開頭的儲存格前面加 `'`，防止 CSV injection |
| [`HtmlText`](src/main/java/io/github/yinyaoqing/jobtext/text/HtmlText.java) | 去除 script / style / 註解，保留段落與清單結構；解碼具名與數字字元參照，非法碼位（0、代理對、超出範圍）換成 U+FFFD；先拆標籤再解碼，讓文字中的 `&lt;b&gt;` 仍以文字呈現 |
| [`SalaryParser`](src/main/java/io/github/yinyaoqing/jobtext/salary/SalaryParser.java) | 具名群組 regex：`月薪65,000~125,000元`、`年薪80~120萬`（單位只寫在後面也套用到下限）、`待遇面議（經常性薪資達4萬元或以上）`、`US$120k–150k / year`、`NT$1.5M`；用 `BigDecimal` 換算、限制位數避免溢位；上下限顛倒就換下一個符合處 |
| [`RemotePolicyParser`](src/main/java/io/github/yinyaoqing/jobtext/text/RemotePolicyParser.java) | 中英文遠端用語分類，並讀出「一週進公司開一次會」「3 days a week in the office」的天數；「不可遠端」優先於其他字眼 |
| [`TimeZoneParser`](src/main/java/io/github/yinyaoqing/jobtext/text/TimeZoneParser.java) | `UTC+8`、`GMT-05:30`、Unicode 減號、`PST` / `CET` 等縮寫、`台灣時間`；`CST`、`IST` 有多種意思，列為「不明確」而不猜 |
| [`TechStack`](src/main/java/io/github/yinyaoqing/jobtext/text/TechStack.java) | 用 lookbehind / lookahead 自訂邊界，所以 `熟悉Java文本` 抓得到 Java，`JavaScript` 不會被算成 Java，`PostgreSQL` 不會被算成 SQL，`Node.js` 的 `js` 不會被算成 JavaScript |
| [`InputSanitizer`](src/main/java/io/github/yinyaoqing/jobtext/text/InputSanitizer.java) | 移除控制字元、零寬字元與雙向覆寫字元（U+202E 等可讓文字看起來和實際內容不同） |
| [`PostingCsv`](src/main/java/io/github/yinyaoqing/jobtext/posting/PostingCsv.java) | 欄位檢查、長度與列數上限、重複 id 回報第一次出現的行號；壞的列略過並記錄，只有表頭錯誤或引號未關閉才中止 |
| [`MarkdownReport`](src/main/java/io/github/yinyaoqing/jobtext/report/MarkdownReport.java) | 表格儲存格跳脫 `\ |` 並把 `& < >` 編碼，輸入中的 HTML 不會在報表中被渲染 |
| [`TextFiles`](src/main/java/io/github/yinyaoqing/jobtext/cli/TextFiles.java) | `CharsetDecoder` 嚴格解碼，遇到非法位元組回報位置並提示 `--encoding Big5`，不默默變成亂碼；輸出先寫暫存檔再原子性搬移，中途失敗不會留下半份報表 |

## 已知限制

- 每筆職缺只取第一個符合的薪資，例如「$100 monthly bonus, $70k」會取到 $100。
- 換算月薪假設每月 176 小時、22 天；年薪除以 12，不考慮台灣常見的 13–14 個月。
- 技術關鍵字是固定清單，不做同義詞推論。

## 開發

```bash
./mvnw verify       # -Xlint:all -Werror，89 個測試
```

CI（GitHub Actions）會在 Java 21 上執行測試，再用範例資料跑一次工具，並比對輸出與 `examples/report.md` 是否一致。
