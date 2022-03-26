package org.example.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;


public class AlipayConfig {

    /**
     * 支付宝网关地址  TODO
     */
    public static final String PAY_GATEWAY = "https://openapi.alipaydev.com/gateway.do";

    /**
     * 支付宝 APPID TODO
     */
    public static final String APPID = "2021000117688721";

    /**
     * 应用私钥 TODO
     */
    public static final String APP_PRI_KEY = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCEwKVAklAOGYWBcHOxfw/Adt0gBbFpwSUfln8eTVMWYrCxszVi3eys9VzOTdaEahmSpAjWp3nMUr+bIZEj5UdDnDrKBLUt9OgGJi/k956FGVrW0pnKF3la82IGOHkt3AaJBmkYq/G251s45DFdFLSg3KMh9+fsz7HyG+6SSF7lz0dhFKrXrbt32ecL/oIMjU2QUvJpot8+Ak6Kv3xDOu1615sQPSbe2My77+4squs+nX1ll1Wb7Gf2RTXFZ5PR70FhBDacInfs5kqQK2lamFHLqaBRNRO9BNkfxgUrbsMkonIRrR7xtpwGjdwgRciYJUTSBfTIq48nwCj7nuwb3kODAgMBAAECggEAPil3OCVLF08cFOBzTEoTNixynuwLh1nJ6GCwZaAPqAvOcWdZOFJy4QqDypboPFjyYJgvC1Sg0+xrjFjLfeN+20nsvWw26q4jd72p762DrQ+SWcWD/f2/9bbBz+fh1neRF9jInaTmKp9rN9iixNNNQYYPXXiYQWuAB77Ml/MLfNBAOQzw9c5L2ZwY7jJAXSnXt6EolQupnaCNZ9i9wMTD+6WA85ienfRU8wmhmy5IHNGOW5O9txk8o9tdDxxqsv2CuN0qA4nDg8U7bJxmSHkc00G7nJz3kO6lOUzjMI7DNSg1A977NAh1J5ccwvC6SD3BKliW5bWX9/KrvdnKev7eAQKBgQDvEoH2q3xz6wmNcX/pe7PxnZod0mLn9BWTdUJL/CyoU7pM2AFuvVR5OKef1Dd/EdpSovxN7EdIxXxHjYDs8yHhY9IqJhhpVZfuWh7om0GbkxZaGBd6aurB8R+SqMtwCIqPD0825RGsTM90p8u6MX0DIFpeFscZw7y4cW27tKA86QKBgQCOJvSWySmGN1ND5SVQoQwr4sPF/ZZ3nJfAFfi8vqA/z3o7DqS+WWrPC/hRPeQLtxex/dmNEWwZEziBfAa8oZp5bdjdeRPgB53qnFtcZvwsX9QEkYlGuAXihx/vuovvgbQ7RGb9g2d2ZhZ1qxVw9lcG5gsk/Zm7wEmdhAGaeVEJiwKBgHjudL1RC3gbeU6KDjkN8P0USsQxCwToDA32L53+JpzTFZcPYNIihVt4VBpeH+BwKG7KsTO9hGWEOBR9uKyjZAz+EG5vrMODn4HbJm1lM6+9nH3gV+f9s1h9Xwoxxg6a03QAO4K6JtFwXTRWOJCrvd/Z+rxDa11WDN2Dyw07Fh8ZAoGAbvI8h8stW5GmrSEVRJ7NbxsnjUnkvCo9zi/X2Qg27layoZFGR0JmuH4xKsdRS0t1dPgZaZ6lbdSwj28LmwHOHOP9FK5BMuL+/3NVYwMF4mRQh9hG+ij6ftjTtjk0qpPi5Tb34vlCMQhwkxoaO/Yn6jx1ikNcT0s+qW+0CKqS8w0CgYAom+LGpTA8hGIjYC2jBJHhSWuU6uX5p+KRcrC5L58zaj5H+z9kOyB9zE1VatYXzD7857Q987XmxJD4KjWXod7xYP3R3mlPdBhIdA+vMm61XDKeL076kCO+17/buPhk6x7IUkrpuGodPKuBR/wYZfCz+J11iywrrVriW04TuRxJHA==";

    /**
     * 支付宝公钥 TODO
     */
    public static final String ALIPAY_PUB_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiw+2PjPSPlxb8tJ5X5Uh3Q4KA04vAe6AVgGKyXs/R4Mtzr5Bkt7Tij2pMaNDc6UyvMJiXx3xsAMtKdhx5j61ocR93h14aBwLB2y4COA/Kh1EaO1eolKX7DWuvG4xay1RC4IUMbnQcNpXboHssBLp9lJEy49gnUcYOuQJuzDMiOjJ/FQrz4ONaY5MAg/VLZKAQwAQRP16vtdNTzjnvBf0hbJ29K7pn8/RHtlQZzvWzTiBvCfaHXzgGyh+iKkD25FiZ+oa2eHlPXhq1aPrKq4E8ZkJoNTrlo6acTPZJJyQyvloC9g6NTTmwBrza8bHpf0UZ8BeqTQ7sBupFGFwDQM0kwIDAQAB";

    /**
     * 签名类型
     */
    public static final String SIGN_TYPE = "RSA2";

    /**
     * 字符编码
     */
    public static final String CHARSET = "UTF-8";

    /**
     * 返回参数格式
     */
    public static final String FORMAT = "json";

    /**
     * 构造函数私有化
     */
    private AlipayConfig() {

    }

    private volatile static AlipayClient instance = null;
    
    /**
     * 单例模式获取, 双重锁校验
     */
    public static AlipayClient getInstance() {
        if (instance == null) {
            synchronized (AlipayConfig.class) {
                if (instance == null) {
                    instance = new DefaultAlipayClient(PAY_GATEWAY, APPID, APP_PRI_KEY, FORMAT, CHARSET, ALIPAY_PUB_KEY, SIGN_TYPE);
                }
            }
        }
        return instance;
    }


}
