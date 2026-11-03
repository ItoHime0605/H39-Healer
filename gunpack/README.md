# H-39 TaCZ 槍包｜0.1.1-alpha

Minecraft 1.21.1／NeoForge／TaCZ 1.1.8 Hotfix R7。

## 已寫入 GitHub 的實際資源

- 原創 H-39 槍身幾何模型，包含槍管、槍托、瞄準鏡、紅十字與四個獨立醫療藥管骨骼。
- 醫療藥管道具的原創 3D 幾何模型。
- TaCZ 格式的射擊、瞄準、換彈、拿出、收起與檢視動畫資料。
- 可用純 Python 3 標準函式庫產生的 UV 貼圖及槍枝／藥管圖示。
- 獨立槍包 ZIP 及模組 JAR 內嵌資源的建置腳本。
- 醫療藥管在槍枝工坊的候選合成配方。

### 自動產生圖片與槍包

在專案根目錄執行：

```bash
python3 tools/generate_h39_gunpack.py --zip dist/H39-Healer-Gunpack.zip
```

GitHub Actions 的 `build-mod.yml` 會先跑檢查，再嘗試完整 NeoForge 21.1／Java 21 編譯。
資源會打包於 JAR 的 `assets/tacz/custom/h39_healer/`，另外產出可獨立安裝的槍包 ZIP。

## 已確定的規格

- 共用彈藥計數 0～20；HUD 目標是 `10/20`。
- 醫療藥管每管補充 5 發；只在至少有 5 發空間時消耗一管。
- 可見藥管數 `ceil(剩餘發數/5)`。
- 一般生物：先由 TaCZ 造成 0.5 HP 穿刺，存活才治療 8 HP，並於 10 秒內每秒回復 2 HP。
- 不死生物：轉為即時與持續傷害；不清除中毒或感染。

## 尚未完成／不得宣稱完成

**不可視為可安裝槍包或正式版。**

目前原創模型、貼圖與動畫已有來源及產生方式，**但藥管骨骼依照即時彈數隱藏、射擊每第五發拋管、2×／4×／6× 瞄準鏡切換及換彈動畫同步尚未完成整合**。
換彈事件已有 NeoForge／TaCZ 候選程式，還須通過完整編譯與遊戲內驗證；不能以純邏輯測試取代實測。
模組主類別已呼叫 TaCZ 官方提供的 `ResourceManager.registerExportResource`，將 JAR 的 `/assets/tacz/custom/h39_healer` 登記成額外槍包來源；TaCZ 的 `GunPackLoader` 會於首次載入將其匯出至遊戲目錄下 `tacz/h39_healer`，然後掃描。這條整合路徑已由 TaCZ R7 位元碼核對，但仍需實際遊戲驗證。若玩家在 TaCZ 設定啟用了覆寫／禁止初次匯出選項，則可能需要使用獨立槍包 ZIP。

所有第三方 TaCZ 與 Meds and Herbs JAR 均不會被上傳到公開 GitHub。
