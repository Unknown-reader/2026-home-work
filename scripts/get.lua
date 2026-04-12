local counter = 1
local threads = {}

function setup(thread)
    thread:set("id", counter)
    table.insert(threads, thread)
    counter = counter + 1
end

function init(args)
    math.randomseed(os.time() + id)
end

function request()
    local key = "key" .. math.random(1, 1000)
    return wrk.format("GET", "/v0/entity?id=" .. key)
end
