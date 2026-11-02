# H-39 完整建置阻礙與已核對的 API

## 不需要使用者重新上傳的檔案

開發環境已有使用者提供的修改版 TaCZ 1.1.8 Hotfix R7（約 55 MB）與 Meds and Herbs 1.2.1（約 947 KB）。它們只供本地驗證，不得提交至公開 GitHub 倉庫。

## 已用 javap 核對的 TaCZ API

- `GunReloadEvent`：`getEntity()`、`getGunItemStack()`、`getLogicalSide()`；實作 `ICancellableEvent`。
- `IGun`：`getIGunOrNull(ItemStack)`、`getGunId(ItemStack)`、`getCurrentAmmoCount(ItemStack)`、`setCurrentAmmoCount(ItemStack,int)`。
- `IAmmo`：`getIAmmoOrNull(ItemStack)`、`getAmmoId(ItemStack)`。
- `GunFireEvent` 與 `GunFinishReloadEvent` 存在；前者有 `getShooter()`、`getGunItemStack()`，後者有 `getGunItemStack()`。

上述檢查只能證明 **TaCZ 方法簽章**，不等於整個 NeoForge 專案編譯成功。

## 目前未完成的工作

1. GitHub 倉庫沒有 Gradle Wrapper，環境也沒有安裝 Gradle，且曾遇到下載站 DNS 解析失敗；未能執行 `gradle build`。
2. `build.gradle` 使用 `compileOnly files('libs/tacz.jar')`，但公開倉庫沒有 `libs/tacz.jar`，因此 GitHub Actions 尚無完整 Java 模組建置依賴。應採用授權的 Maven 依賴，或於使用者本地放置自己的 JAR；不要將第三方 JAR 上傳公開倉庫。
3. H-39 換彈橋接目前會取消原生 `GunReloadEvent`，並立即扣除醫療藥管與寫入彈數。**這尚未驗證動畫、同步、換彈時間或事件順序**，不能視為完成。
4. 槍包的 3D 模型、貼圖與動畫尚未齊備；候選 JSON 雖與 TaCZ 內建槍包結構部分對照，仍需載入驗證。
5. 需實測醫療效果 ID、目標死亡順序與不死生物受傷無敵幀。

## 最小可行驗證路徑

- 在可下載 Gradle 與 NeoForge 依賴的環境建立 Wrapper。
- 使用開發者自己的 TaCZ JAR 作為 `libs/tacz.jar`，執行 `./gradlew compileJava`。
- 修正所有實際編譯錯誤，接著執行 `./gradlew build`。
- 加入資源與遊戲內測試後，才交付正式 JAR。

使用者目前不必再上傳相同的 TaCZ 或 Meds and Herbs JAR。

## 2026-11-03 實際 JAR 驗證補充

已以 `javap -c -p` 檢查使用者提供的 Meds and Herbs 1.2.1 JAR 內 `com.plank.meds_and_herbs.init.Effects` 的註冊字串，**六種傷勢效果 ID 全部確認存在**：

- `meds_and_herbs:bleeding`
- `meds_and_herbs:internal_bleeding`
- `meds_and_herbs:blood_loss`
- `meds_and_herbs:burns`
- `meds_and_herbs:laceration`
- `meds_and_herbs:bone_fracture`

`GunReloadEvent` 已從修改版 TaCZ JAR 驗證為 `ICancellableEvent`；`IGun` 的讀取與寫入目前彈數方法也已核對。這些仍不等於完整 NeoForge 編譯成功。
