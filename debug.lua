-- HRP4 protocol
-- Declare protocol
hrp4_proto = Proto("HRP4", "HRP4 Protocol")

-- create function to dissect
function hrp4_proto.dissector(buffer, pinfo, tree)
    pinfo.cols.protocol = "HRP4"
    local sVersion = "HRP4 "
    local subtree = tree:add(hrp4_proto, buffer(), "HRP4 Data")

    -- Source Addr
    subtree:add(buffer(0, 4), "Packet Type: " .. buffer(0, 4):string())
    -- Source Addr
    subtree:add(buffer(4, 4), "Source Address: " .. tostring(buffer(4, 4):ipv4()))
    -- Destination Addr
    subtree:add(buffer(8, 4), "Destination Address: " .. tostring(buffer(8, 4):ipv4()))
    -- Source Port
    subtree:add(buffer(12, 2), "Source Port: " .. buffer(12, 2):uint())
    -- Destination Port
    subtree:add(buffer(14, 2), "Destination Port: " .. buffer(14, 2):uint())
    -- TTL
    subtree:add(buffer(16, 1), "TTL: " .. buffer(16, 1):uint())

    -- Switch according to datatype.
    datatype = buffer(20, 4):string()

    if datatype == "BCN4" then
        pinfo.cols.info = sVersion .. "Bacon Packet: " .. (buffer:len() - 24) / 12 .. " Routing Entries"
        local inner = subtree:add(hrp4_proto, buffer(20), "Bacon Packet")
        -- Routing Entry
        if buffer:len() > 24 then
            local i = 24
            while buffer:len() - i >= 12 do
                local entry = inner:add(hrp4_proto, buffer(i, 12), "Routing Entry")
                entry:add(buffer(i, 1), "Link Cost: " .. buffer(i, 1):uint())
                entry:add(buffer(i + 3, 1), "TTL: " .. buffer(i + 3, 1):uint())
                entry:add(buffer(i + 4, 4), "Address0: " .. tostring(buffer(i + 4, 4):ipv4()))
                entry:add(buffer(i + 8, 4), "Address1: " .. tostring(buffer(i + 8, 4):ipv4()))
                i = i + 12
            end
        end
    elseif datatype == "RTP4" then
        pinfo.cols.info = sVersion .. "RTP4 Packet"
        local inner = subtree:add(hrp4_proto, buffer(20), "RTP4 Packet")
        inner:add(hrp4_proto, buffer(24, 4), "Sequence Num: " .. buffer(24, 4):uint())
        inner:add(hrp4_proto, buffer(28, 4), "Acknowledgment Num: " .. buffer(28, 4):uint())
        local flags = inner:add(hrp4_proto, buffer(29, 1), "Flags")
        flags:add(hrp4_proto, buffer(29, 1), "SYN: " .. buffer(29, 1):bitfield(0, 1))
        flags:add(hrp4_proto, buffer(29, 1), "ACK: " .. buffer(29, 1):bitfield(1, 1))
        flags:add(hrp4_proto, buffer(29, 1), "FIN: " .. buffer(29, 1):bitfield(2, 1))
        flags:add(hrp4_proto, buffer(29, 1), "RST: " .. buffer(29, 1):bitfield(3, 1))

        inner:add(hrp4_proto, buffer(30, 2), "Window Size: " .. buffer(30, 2):uint())
        inner:add(hrp4_proto, buffer(32), "Payload: " .. buffer(32):string())
    else
        pinfo.cols.info = sVersion .. "Unknown Payload: " .. buffer(20):string()
        local inner = subtree:add(hrp4_proto, buffer(20), "Unknown Packet")
        inner:add(hrp4_proto, buffer(20), "Payload: " .. buffer(20):string())
    end
end

-- load the udp.port table
udp_table = DissectorTable.get("udp.port")
-- register our protocol to handle udp port 5555
udp_table:add(1337, hrp4_proto)