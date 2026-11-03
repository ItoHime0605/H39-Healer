# H-39 0.1.17-alpha｜真正執行 TaCZ Lua 狀態機測試

## 已確認並修復的問題

舊版把 `last_rounds` 與 `last_visible` 宣告成 Lua 模組層級變數。多位玩家、切換槍枝或同時顯示不同 H-39 時，兩把槍會共用同一個計數。使用 TaCZ 1.1.8 Hotfix R7 JAR 所附 LuaJ 執行以下順序，**0.1.16-alpha 確實測試失敗**：

1. H-39 A 由 20 發逐步射至 16 發。
2. 初始化 H-39 B，同樣有 20 發。
3. H-39 A 再射一發至 15 發。
4. 預期 A 彈出第一管，舊程式因 B 重置共用計數而沒有彈出。

新版將狀態放在 Lua weak-key 表 `state_per_context[context]`，每個 TaCZ 動畫上下文各有其 `last_rounds`／`last_visible`。不再操作 TaCZ 共用動畫軌道計數；不更動任何第三方 TaCZ JAR。

## 自動化測試

```bash
python3 tools/h39_lua_runtime_test.py /path/to/tacz-r7.jar
```

程式會從 TaCZ JAR 暫時抽出原生 `default_state_machine.lua` 與 LuaJ core/jse，利用 LuaJ 模擬：
- 右鍵瞄準 ADS 狀態、射擊、換彈及動作軌道是否有效
- 兩把 H-39 互相穿插初始化後，拋管判定仍各自正確
- 射完 20 發恰好拋出 4 管；一般每發射擊不拋出普通彈殼
- 換彈、遠端彈數跳動與重新裝備，不會拋出幽靈空管

測試被納入 GitHub Actions 完整 NeoForge 建置。檢測是由模擬的 TaCZ context 提供方法，能找出 Lua 腳本例外與狀態污染，**無法取代真實用戶端渲染、網路連線或第一人稱射擊測試**。右鍵斷線仍需用戶端 `logs/latest.log` 或 `disconnect-*.txt` 定案。
