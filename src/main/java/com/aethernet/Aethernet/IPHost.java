package com.aethernet.Aethernet;

import com.aethernet.*;
import com.aethernet.config.EthConfig;
import com.aethernet.config.utils.ConfigTermTemplate;

public class IPHost {
    
    String name;

    public ConfigTermTemplate<String> ipAddr;

    public IPHost(String hostName) {
        this.name = hostName;
        ipAddr = new ConfigTermTemplate<String>(hostName + ".ipAddr", "172.0.0.1", false, EthConfig.configTermsMap, () -> {});
    }
}
