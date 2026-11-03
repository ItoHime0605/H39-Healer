-- H-39 TaCZ state machine: 5 rounds per tube, no conventional shell casing.
-- Extends the verified TaCZ 1.1.8 Hotfix R7 default state machine.
local default = require("tacz_default_state_machine")
local STATIC_TRACK_LINE = default.STATIC_TRACK_LINE
local tube_track = default.static_track_top.value
default.static_track_top.value = tube_track + 1

-- The default state machine ejects a shell for EVERY INPUT_SHOOT.
-- H-39 has no brass casings: suppress that default action, while leaving
-- regular shooting animation handling to the default gun-kick state.
local base_idle_state = default.main_track_states.idle
local idle_state = setmetatable({}, {__index = base_idle_state})
function idle_state.transition(this, context, input)
    if input == INPUT_SHOOT then
        return this.main_track_states.idle
    end
    return base_idle_state.transition(this, context, input)
end

local function bounded_rounds(context)
    local n = context:getAmmoCount()
    if n == nil then return 0 end
    return math.max(0, math.min(20, n))
end

local function tube_count(rounds)
    return math.floor((rounds + 4) / 5)
end

local tube_state = {last_rounds = -1, last_visible = -1}

function tube_state.entry(this, context)
    this.tube_state.last_rounds = -1
    this.tube_state.last_visible = -1
end

function tube_state.update(this, context)
    local rounds = bounded_rounds(context)
    local visible = tube_count(rounds)
    local state = this.tube_state

    -- Only eject when a shot crosses 16->15, 11->10, 6->5, or 1->0.
    -- Reload or switching weapons must NEVER eject a tube.
    -- A real semi-auto shot consumes exactly one round. Creative commands,
    -- inventory corrections or delayed server sync can jump multiple rounds;
    -- those changes must not produce phantom empty tubes.
    if state.last_rounds >= 0 and rounds == state.last_rounds - 1 then
        if tube_count(rounds) < tube_count(state.last_rounds) then
            context:popShellFrom(0)
        end
    end

    if visible ~= state.last_visible then
        local track = context:getTrack(STATIC_TRACK_LINE, tube_track)
        context:runAnimation("tubes_" .. visible, track, true, PLAY_ONCE_HOLD, 0)
        state.last_visible = visible
    end
    state.last_rounds = rounds
end

local M = setmetatable({
    main_track_states = setmetatable({idle = idle_state}, {__index = default.main_track_states}),
    tube_state = tube_state,
}, {__index = default})

function M:initialize(context)
    default.initialize(self, context)
    self.tube_state.last_rounds = -1
    self.tube_state.last_visible = -1
end

function M:states()
    local states = default.states(self)
    table.insert(states, self.tube_state)
    return states
end

return M
