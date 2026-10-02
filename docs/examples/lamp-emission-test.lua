-- Copie para um computador CC:Tweaked conectado ao peripheral modular_table.
-- Uso: lamp-emission-test [nome ou lado do peripheral]
local name = ...
local panel = name and peripheral.wrap(name) or peripheral.find("modular_table")
if not panel or type(panel.getOutputs) ~= "function" then
    error("Nenhum peripheral modular_table encontrado", 0)
end

local lamps = {}
for _, output in ipairs(panel.getOutputs()) do
    if output.type == "lamp" or output.type == "lamp_2x1" then lamps[#lamps + 1] = output end
end
table.sort(lamps, function(a, b)
    if a.row ~= b.row then return a.row < b.row end
    return a.column < b.column
end)
if #lamps == 0 then error("Instale ao menos um modulo de lampada antes do teste", 0) end

local ok, err = pcall(function()
    for _, lamp in ipairs(lamps) do
        print(("Lampada na coluna %d, linha %d"):format(lamp.column, lamp.row))
        for intensity = 0, 15 do
            if not panel.setLampTestIntensity(lamp.column, lamp.row, intensity) then
                error("Nao foi possivel atualizar a lampada na coluna " .. lamp.column .. ", linha " .. lamp.row, 0)
            end
            print(("  Intensidade visual: %02d/15"):format(intensity))
            sleep(0.5)
        end
        panel.clearLampTestIntensity(lamp.column, lamp.row)
    end
end)

-- Sempre remove o override temporario, inclusive se o teste for interrompido.
for _, lamp in ipairs(lamps) do
    pcall(panel.clearLampTestIntensity, lamp.column, lamp.row)
end
if not ok then error(err, 0) end
print("Teste concluido; intensidades normais restauradas.")
