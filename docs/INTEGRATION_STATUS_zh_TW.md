# H-39 整合驗證紀錄

## 已核對的第三方 API

以使用者提供的 `tacz-neoforge-1.21.1-1.1.8-hotfix-r7_zonezero_fixed_ids(1).jar` 執行 `javap`：

- `com.tacz.guns.api.event.common.EntityHurtByGunEvent.Pre` 存在，實作 `ICancellableEvent`。
- `Pre#setBaseAmount(float)` 存在，可調整槍彈基礎傷害。
- `EntityHurtByGunEvent.Post` 存在。
- 兩種事件都有 `getGunId()`、`getHurtEntity()`、`getLogicalSide()` 等父類別存取器。

Meds and Herbs 1.2.1 JAR 的 `neoforge.mods.toml` 確認模組 ID 為 `meds_and_herbs`，且含 `Bleeding`、`InternalBleeding`、`BoneFracture` 類別。

## 尚未解決的風險

1. **已確認為預期設計，非 BUG：** H-39 使用 `Post` 命中後治療。0.5 HP 穿刺傷害先結算；如果目標因此死亡，就維持死亡、不治療也不復活。只有仍存活的目標才獲得即時及持續治療。
2. 尚未證實 `meds_and_herbs` 六種效果的實際註冊 ID 全部與字串相符；需核對登錄類別。
3. 不死生物受到額外 `hurt()` 時，可能受 Minecraft 無敵幀影響；必須在遊戲內測試。
4. 槍包的 `data`、`display`、材質、動畫與完整換彈邏輯仍不完整。
5. Gradle 建置尚未成功，`build.gradle` 的依賴及插件版本仍待實際 CI 驗證。

## 交付原則

原本 TaCZ JAR 不修改。預計以獨立 NeoForge 模組 JAR 搭配 TaCZ 槍包；若驗證 TaCZ 可從獨立模組 JAR 載入槍包，再整合成單一附加 JAR。任何尚未驗證的方式均不得宣稱已可安裝。
