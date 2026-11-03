# H-39 單一彈藥計數與 TaCZ 槍膛相容

## 實際問題

使用者要求 4 根醫療藥管，每管 5 發，始終只顯示一個 `0–20` 的剩餘彈數。前一版本使用 `closed_bolt`：TaCZ 會在槍內另外維護 `hasBulletInBarrel`，並在上膛時先扣除彈匣的一發。於是槍內可射擊總數不一定等於 `getCurrentAmmoCount()`，HUD、四根藥管骨骼與每五發拋管的 Lua 判定可能互相矛盾。

## 修正

- 更改 `gunpack/data/h39/data/guns/h39_data.json` 的 `bolt` 為 `open_bolt`。
- TaCZ R7 在 `ModernKineticGunScriptAPI#reduceAmmoOnce` 處理 open bolt 時，直接從當前彈數扣 1；不建立額外的槍膛彈。這樣 H-39 的 HUD、每五發空管拋出，以及伺服器定時補管都共用同一個 `getCurrentAmmoCount()`。
- 不改槍枝外觀、射速、醫療效果、傷害與彈道：仍為半自動 180 RPM、初始 0.5 HP、醫療彈速度 200、重力 0.03。
- 換彈可使用背包內或副手欄位的醫療藥管；伺服器在換彈結束時再次檢查並扣除。

此修正已根據使用者提供的 TaCZ Hotfix R7 JAR 位元碼核對分支邏輯；實際遊戲客戶端預測與多人同步仍需測試。
