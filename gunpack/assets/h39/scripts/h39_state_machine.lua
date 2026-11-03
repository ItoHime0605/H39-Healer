-- H-39 original TaCZ state machine: front-to-back tube visibility.
-- TaCZ 1.21.1 hotfix-r7 inherits its official default state machine.
local default = require("tacz_default_state_machine")
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local tube_track = default.static_track_top.value
default.static_track_top.value = tube_track + 1

local tube_state = { last_visible = -1 }

local function visible_count(context)
    local rounds = context:getAmmoCount()
    if rounds == nil then rounds = 0 end
    rounds = math.max(0, math.min(20, rounds))
    return math.floor((rounds + 4) / 5)
end

function tube_state.entry(this, context)
    this.tube_state.last_visible = -1
end

function tube_state.update(this, context)
    local count = visible_count(context)
    if count == this.tube_state.last_visible then return end
    local track = context:getTrack(STATIC_TRACK_LINE, tube_track)
    -- HOLD the geometry scale until the next ammo-count change.
    context:runAnimation("tubes_" .. count, track, true, PLAY_ONCE_HOLD, 0)
    this.tube_state.last_visible = count
end

local M = setmetatable({tube_state=tube_state}, {__index=default})

function M:initialize(context)
    default.initialize(self, context)
    self.tube_state.last_visible = -1
end

function M:states()
    local states = default.states(self)
    table.insert(states, self.tube_state)
    return states
end

return M
