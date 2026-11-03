# H-39 編譯與實際整合狀態（2026-11-03）

## 已完成：真正的 GitHub Actions NeoForge 編譯

先前「缺 Gradle、公開倉庫無 TaCZ JAR」的阻礙已排除：

1. GitHub Actions 使用 `gradle/actions/setup-gradle@v4` 取得 Gradle 8.14.3，Java 為 21。
2. Gradle 使用 Modrinth Maven 上的 TaCZ 1.21.1 NeoForge Hotfix R7（`maven.modrinth:OypNE65K:62q6mB5Q`），不把任何第三方 JAR 上傳公開 GitHub。
3. 如果專案本地有 `libs/tacz.jar`，則優先用玩家自己的修改版 TaCZ 來編譯。
4. `generateH39Gunpack` 會產生原創 PNG、圖示及嵌入 JAR 的槍包資源。
5. **GitHub Actions Gradle 建置已確實成功**，例如：
   - <https://github.com/ItoHime0605/H39-Healer/actions/runs/41551061438> （Commit `a210f171`，NeoForge build: success）。
   - <https://github.com/ItoHime0605/H39-Healer/actions/runs/41551061462> （槍包 Python 測試: success）。
   - <https://github.com/ItoHime0605/H39-Healer/actions/runs/41551061429> （Java 彈藥邏輯測試: success）。

以上成功的版本已包含原創 H-39 模型、藥管幾何、PNG 產生器、Lua 彈藥數量顯示及 Java TaCZ 槍包匯出整合。

**成功編譯不代表 Minecraft 實際載入、遊戲玩法或動畫已實測通過。**

## 一個 JAR 安裝的技術路徑

已用使用者提供的修改版 TaCZ R7 JAR `javap` 核對：

- `com.tacz.guns.api.resource.ResourceManager.registerExportResource(Class<?>,String)` 為可用 API。
- TaCZ `GunPackLoader` 在首次掃描會讀取 `ResourceManager.EXTRA_ENTRIES`，將第三方 JAR 內的指定資源匯出至遊戲目錄 `tacz/<名稱>`。
- H-39 主類別現已登記 `/assets/tacz/custom/h39_healer`，建置流程也確實放入該目錄。
- TaCZ 設定的首次匯出覆寫選項可能改變匯出行為；另有獨立 ZIP 備援。

## 尚待實際遊戲驗證或實作

- 醫療傷害和持續治療正確觸發、六種外傷清除、無疊加刷新。
- TaCZ 射擊、換彈取消與「每管五發」背包消耗、客戶端同步、換彈動畫與時間。
- Lua 的 `context:getAmmoCount()` 是否能讓 0～4 個醫療藥管骨骼動態隱藏。
- 2×／4×／6× 瞄準倍率切換、每第五發拋藥管的模型特效尚未完成。
- 以使用者本人修改的 TaCZ JAR 進行遊戲啟動與伺服器連線的相容性測試。

## 已核對的 Meds and Herbs ID

以下六個效果均從玩家提供的 Meds and Herbs 1.2.1 JAR 核對：
`bleeding`, `internal_bleeding`, `blood_loss`, `bone_fracture`, `laceration`, `burns`。

以下六項藥管原料均在相同 JAR 的資源模型／配方路徑中找到對應名稱：
`aloe_leaf`, `chamomile`, `plantago_leaf`, `glass_tube`，以及原版 Minecraft 的 `glistering_melon_slice` 與 `iron_ingot`。
