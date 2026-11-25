-- Atomic Token Bucket.
-- KEYS[1] = bucket key
-- ARGV[1] = capacity (max burst)
-- ARGV[2] = refill rate (tokens per second)
-- ARGV[3] = tokens requested for this call
-- Returns { allowed (1|0), tokens_remaining, retry_after_ms }
--
-- Redis TIME is used instead of the client clock so that every gateway
-- instance shares one time source (no clock skew between app servers).

local key         = KEYS[1]
local capacity    = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local requested   = tonumber(ARGV[3])

local t   = redis.call('TIME')
local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)

local data   = redis.call('HMGET', key, 'tokens', 'ts')
local tokens = tonumber(data[1])
local ts     = tonumber(data[2])

if tokens == nil or ts == nil then
  tokens = capacity
  ts = now
end

local elapsed_ms = math.max(0, now - ts)
tokens = math.min(capacity, tokens + (elapsed_ms / 1000) * refill_rate)

local allowed = 0
local retry_after_ms = 0

if tokens >= requested then
  tokens = tokens - requested
  allowed = 1
else
  retry_after_ms = math.ceil(((requested - tokens) / refill_rate) * 1000)
end

redis.call('HSET', key, 'tokens', tostring(tokens), 'ts', now)
-- Idle buckets expire after twice the time needed to fully refill.
redis.call('PEXPIRE', key, math.ceil((capacity / refill_rate) * 1000) * 2)

return { allowed, math.floor(tokens), retry_after_ms }
