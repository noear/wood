package org.noear.wood.springboot_starter;

import org.noear.wood.annotation.Db;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Db.class)
public class Weed3DsAutoConfigure {
    @Bean
    @ConditionalOnMissingBean(Db.class)
    public Object initChyRpcApplication(){

        return null;
    }
}
