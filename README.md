# H-39「治癒者」 / H39-Healer

Minecraft 1.21.1 / NeoForge / Java 21 / TaCZ 1.1.8 Hotfix R7 / Meds and Herbs 1.2.1.

**開發中，尚無可安裝的正式版。**

## 設計規格
- 半自動醫療步槍，180 RPM，4 管醫療藥管，每管 5 發，共 20 發；可逐管補充。
- 對一般生物：0.5 HP 穿刺、8 HP 即時治療、每秒 2 HP 持續 10 秒；重複命中刷新而不疊加。
- 對不死生物：相同穿刺傷害，將治療轉為 8 HP 即時傷害與每秒 2 HP 持續 10 秒。
- 清除指定外傷，不清除感染與中毒。
- 醫療藥管配方：蘆薈葉×5、洋甘菊×5、車前草葉×5、玻璃管×2、閃爍西瓜片×5、鐵錠×1。

## 0.1.1-alpha 開發狀態
已加入 H-39 原創槍枝與醫療藥管幾何模型、純 Python 產生的 PNG 貼圖與圖示、TaCZ 動畫與 Lua 藥管顯示狀態機；Java 端已註冊 TaCZ 額外槍包匯出 API，預計使用單一 JAR 自動將槍包匯入遊戲目錄。GitHub Actions 現在會嘗試真正的 NeoForge Gradle 編譯。**完整編譯與遊戲相容性尚未得到成功驗證，請勿將此版本當成正式可遊玩版。**

槍包詳情請參閱 [gunpack/README.md](gunpack/README.md)；TaCZ 公開 Maven 編譯依賴不包含你的個人修改版，若要用自己的 TaCZ JAR 進行本地編譯，請放到 `libs/tacz.jar`。

第三方 TaCZ、Meds and Herbs JAR 不會提交到本儲存庫。