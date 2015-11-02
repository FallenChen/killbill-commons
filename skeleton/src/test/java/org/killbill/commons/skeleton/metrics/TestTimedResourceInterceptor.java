/*
 * Copyright 2010-2014 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.commons.skeleton.metrics;

import java.net.URI;

import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.codahale.metrics.Timer;
import org.killbill.commons.metrics.MetricTag;
import org.killbill.commons.metrics.TimedResource;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.matcher.Matchers;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

@Test(groups = "fast")
public class TestTimedResourceInterceptor {

    private MetricRegistry registry;
    private TestResource interceptedResource;

    @BeforeMethod
    public void setup() throws Exception {
        final Injector injector = Guice.createInjector(new TestResourceModule());
        registry = injector.getInstance(MetricRegistry.class);
        interceptedResource = injector.getInstance(TestResource.class);
    }

    public void testResourceWithResponse() {
        final Response response = interceptedResource.createOk();
        Assert.assertEquals(200,  response.getStatus());

        final Timer timer = registry.getTimers().get("kb_resource.path.createOk.POST.2xx.200");
        Assert.assertNotNull(timer);
        Assert.assertEquals(1, timer.getCount());
    }

    public void testResourceSimpleTag() {
        final Response response = interceptedResource.createOk("AUTHORIZE");
        Assert.assertEquals(200, response.getStatus());

        final Timer timer = registry.getTimers().get("kb_resource.path.createOk.POST.AUTHORIZE.2xx.200");
        Assert.assertNotNull(timer);
        Assert.assertEquals(1, timer.getCount());
    }

    public void testResourceWithPropertyTag() {
        final Response response = interceptedResource.createOk(new Payment("PURCHASE"));
        Assert.assertEquals(201, response.getStatus());

        final Timer timer = registry.getTimers().get("kb_resource.path.createOk.POST.PURCHASE.2xx.201");
        Assert.assertNotNull(timer);
        Assert.assertEquals(1, timer.getCount());
    }

    public void testResourceNullTag() {
        final Response response = interceptedResource.createOk((String) null);
        Assert.assertEquals(200, response.getStatus());

        final Timer timer = registry.getTimers().get("kb_resource.path.createOk.POST.null.2xx.200");
        Assert.assertNotNull(timer);
        Assert.assertEquals(1, timer.getCount());
    }

    public void testResourceNullPropertyTag() {
        final Response response = interceptedResource.createOk((Payment) null);
        Assert.assertEquals(201, response.getStatus());

        final Timer timer = registry.getTimers().get("kb_resource.path.createOk.POST.null.2xx.201");
        Assert.assertNotNull(timer);
        Assert.assertEquals(1, timer.getCount());
    }

    public void testResourceWithNullResponse() {
        final Response response = interceptedResource.createNullResponse();
        Assert.assertNull(response);

        final Timer timer = registry.getTimers().get("kb_resource.path.createNullResponse.PUT.2xx.204");
        Assert.assertNotNull(timer);
        Assert.assertEquals(1, timer.getCount());
    }

    public void testResourceWithVoidResponse() {
        interceptedResource.createNullResponse();

        final Timer timer = registry.getTimers().get("kb_resource.path.createNullResponse.PUT.2xx.204");
        Assert.assertNotNull(timer);
        Assert.assertEquals(1, timer.getCount());
    }

    public void testResourceWithWebApplicationException() {
        try {
            interceptedResource.createWebApplicationException();
            Assert.fail();
        } catch(WebApplicationException e) {
            final Timer timer = registry.getTimers().get("kb_resource.path.createWebApplicationException.POST.4xx.404");
            Assert.assertNotNull(timer);
            Assert.assertEquals(1, timer.getCount());
        }
    }

    public static class Payment {
        private final String type;

        public Payment(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    @Path("path")
    public static class TestResource {

        @TimedResource
        @POST
        public Response createOk() {
            return Response.ok().build();
        }

        @TimedResource
        @POST
        public Response createOk(@MetricTag(tag = "transactionType") String type) {
            return Response.ok().build();
        }

        @TimedResource
        @POST
        public Response createOk(@MetricTag(tag = "transactionType", property = "type") Payment payment) {
            return Response.created(URI.create("about:blank")).build();
        }

        @TimedResource
        @PUT
        public Response createNullResponse() {
            return null;
        }

        @TimedResource
        @PUT
        public void createVoidResponse() {
        }

        @TimedResource
        @POST
        public void createWebApplicationException() {
            throw new WebApplicationException(404);
        }
    }

    public static class TestResourceModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(GuiceContainer.class);
            bind(TestResource.class).asEagerSingleton();
            bind(MetricRegistry.class).asEagerSingleton();

            final TimedResourceListener timedResourceTypeListener =
                    new TimedResourceListener(getProvider(GuiceContainer.class), getProvider(MetricRegistry.class));
            bindListener(Matchers.any(), timedResourceTypeListener);
        }
    }
}