# Avionics Cable Network and Network Bridge

Avionics Cable carries data only. Place segments next to each other to create
lines and branches; any compatible avionics endpoint touching one of the six
faces joins the same network. There are no ports, frequencies, junction blocks,
or wireless links. The Modular Table Base and Network Bridge are endpoints;
flight sensors and the GPWS join the network as they are implemented.

The cable uses a compact center hub and arms toward adjacent cable or endpoint
blocks. It has its own graphite/brass Blockbench model and textures, with
adaptive visual leads on the redesigned Radar Altimeter and Basic Terrain
Scanner. See [visual connections](avionics-cable-visuals.md). The CC:Tweaked
Networking Cable is a UX reference only; this project
does not reuse its code or assets. The `Network Bridge` is the dedicated
CC:Tweaked gateway; endpoint peripherals remain independent.

## Lua API

The `avionics_network_bridge` peripheral provides:

| Method | Return |
| --- | --- |
| `getNetworkStatus()` | `{status, connected, complete, cableCount, endpointCount}` |
| `getConnectedDevices()` | List of `{type, x, y, z}` relative to the bridge |

Possible statuses are `disconnected`, `connected`, `incomplete`, and
`limit_exceeded`. `complete` is false for the last two states. Counts are the
number of blocks observed so far; when a network is incomplete they are lower
bounds, not a claim that the entire topology was inspected. Device enumeration
is empty for incomplete or limited results, so a partial result cannot be
mistaken for a complete device list. Device coordinates are relative to this
bridge in its current level/sublevel and are not internal graph identifiers.
`connected` is true only when the entire reachable topology is confirmed.

The `avionics_network_changed` event is queued on computers attached to the
bridge after the server confirms a changed snapshot. Its arguments are the
attachment name and the same status table returned by `getNetworkStatus()`.
The bridge exposes topology only; each component's measurements and local
events are accessed through that component's own peripheral.

```lua
local bridge = assert(peripheral.find("avionics_network_bridge"), "Bridge não encontrada")
local network = bridge.getNetworkStatus()
print(network.status, network.cableCount, network.endpointCount)

for _, device in ipairs(bridge.getConnectedDevices()) do
    print(device.type, device.x, device.y, device.z)
end

while true do
    local _, name, updated = os.pullEvent("avionics_network_changed")
    print(name, updated.status, updated.cableCount, updated.endpointCount)
end
```

## Limits and persistence

Topology is calculated server-side from the saved cable and endpoint blocks;
the graph itself is not duplicated in saved data. Changes to relevant blocks
and chunk load state invalidate the cache, with multiple changes coalesced at
server tick end. No periodic full-network scan runs while the world is idle.

The server config `maxAvionicsNetworkBlocks` defaults to `4096` visited cable
and endpoint blocks per network. Traversal never loads chunks. If a path reaches
an unloaded chunk, status is `incomplete` and the graph is reconsidered when
chunk state changes. If the configured visit limit is exceeded, status is
`limit_exceeded` and no partial endpoint list is returned. Networks do not
bridge separate levels, dimensions, or sublevels.

## Test status

Automated tests cover graph lines, branches, cycles, endpoints, disconnection,
unloaded boundaries, visit limits, rediscovery after removal/reconnection,
typed reading invariants, bridge methods/events, and the table's lack of
aggregate network methods. In-game validation should connect a bridge, table,
and sensor; exercise branches and removals; and verify Lua topology/events.
