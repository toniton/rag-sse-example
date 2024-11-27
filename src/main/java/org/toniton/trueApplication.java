package org.toniton;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.glassfish.jersey.media.sse.SseFeature;
import org.toniton.sse.SseResource;
import org.toniton.sse.SseResourceJLamaResource;

public class trueApplication extends Application<trueConfiguration> {

    public static void main(final String[] args) throws Exception {
        new trueApplication().run(args);
    }

    @Override
    public String getName() {
        return "true";
    }

    @Override
    public void initialize(final Bootstrap<trueConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final trueConfiguration configuration,
                    final Environment environment) {
        // TODO: implement application

        environment.jersey().register(SseFeature.class);

        // Register your resources
        environment.jersey().register(new SseResource());
        environment.jersey().register(new SseResourceJLamaResource());
    }

}
