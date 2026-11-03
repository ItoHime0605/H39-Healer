# H-39「治癒者」製作配方

Minecraft 1.21.1／NeoForge／TaCZ 1.1.8 Hotfix R7（Zone Zero 廢土伺服器）

已新增 `gunpack/data/h39/recipe/gun/h39.json`。由 **TaCZ 槍枝工坊**製作 1 把 H-39，不是原版 3×3 工作台配方。

| 材料 | 數量 |
|---|---:|
| 鐵錠（`c:ingots/iron`） | 32 |
| 銅錠（`c:ingots/copper`） | 8 |
| 紅石粉 | 8 |
| Zombie Extreme 鋼錠（`zombie_extreme:ignotsteel`） | 3 |
| Meds and Herbs 玻璃管 | 4 |
| KubeJS 瓶蓋盒（`kubejs:bottle_cap_box`） | 4 |
| 鑽石 | 2 |

此成本設定為較高階的醫療狙擊裝備，與使用者廢土服現有的瓶蓋盒經濟和鋼鐵材料一致。槍枝成品不會預裝專用固定瞄具，玩家從 TaCZ 已核對的七款允許配件中自行選配。

醫療藥管的配方仍為：蘆薈葉×5、洋甘菊×5、車前草葉×5、玻璃管×2、閃爍西瓜片×5、鐵錠×1。**每個製作完成的藥管補充 5 發，不是製作一發子彈。**

**相容性說明：** 這份槍枝工坊配方是專為 Zone Zero 設計，需有 KubeJS 所註冊的 `bottle_cap_box`，及 Zombie Extreme 提供的鋼錠；如果移到沒有這些材料的其他伺服器，需要調整配方。H-39 模組 JAR 本身不會將這兩款模組設為 Java 強制依賴。配方 JSON 結構已按使用者提供的 TaCZ 內建 M700/M4A1 工坊配方製作；尚未遊戲內驗證。
