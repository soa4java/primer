package uk.co.epsilontechnologies.primer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Primable {

    private static final Response INVALID_REQUEST = new Response(404, "application/json", "Request Not Primed");

    private final List<PrimedInvocation> primedInvocations = new ArrayList<>();

    private final Server server;

    public Primable(final String contextPath, final int port) {
        this.server = new Server(port);
        this.server.setHandler(new PrimedHandler(contextPath));
    }

    public void start() {
        try {
            server.start();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            primedInvocations.clear();
            server.stop();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Action get(final String uri, final Parameters parameters, final Headers headers) {
        return new Action(this, new Request("GET", uri, "", parameters, headers));
    }

    public Action get(final String uri, final Headers headers) {
        return get(uri, new Parameters(), headers);
    }

    public Action get(final String uri, final Parameters parameters) {
        return get(uri, parameters, new Headers());
    }

    public Action get(final String uri) {
        return get(uri, new Parameters(), new Headers());
    }

    public Action head(final String uri, final Parameters parameters, final Headers headers) {
        return new Action(this, new Request("HEAD", uri, "", parameters, headers));
    }

    public Action head(final String uri, final Headers headers) {
        return head(uri, new Parameters(), headers);
    }

    public Action head(final String uri, final Parameters parameters) {
        return head(uri, parameters, new Headers());
    }

    public Action head(final String uri) {
        return head(uri, new Parameters(), new Headers());
    }

    public Action trace(final String uri, final Parameters parameters, final Headers headers) {
        return new Action(this, new Request("TRACE", uri, "", parameters, headers));
    }

    public Action trace(final String uri, final Headers headers) {
        return trace(uri, new Parameters(), headers);
    }

    public Action trace(final String uri, final Parameters parameters) {
        return trace(uri, parameters, new Headers());
    }

    public Action trace(final String uri) {
        return trace(uri, new Parameters(), new Headers());
    }

    public Action options(final String uri, final Parameters parameters, final Headers headers) {
        return new Action(this, new Request("OPTIONS", uri, "", parameters, headers));
    }

    public Action options(final String uri, final Headers headers) {
        return options(uri, new Parameters(), headers);
    }

    public Action options(final String uri, final Parameters parameters) {
        return options(uri, parameters, new Headers());
    }

    public Action options(final String uri) {
        return options(uri, new Parameters(), new Headers());
    }

    public Action delete(final String uri, final Parameters parameters, final Headers headers) {
        return new Action(this, new Request("DELETE", uri, "", parameters, headers));
    }

    public Action delete(final String uri, final Headers headers) {
        return delete(uri, new Parameters(), headers);
    }

    public Action delete(final String uri, final Parameters parameters) {
        return delete(uri, parameters, new Headers());
    }

    public Action delete(final String uri) {
        return delete(uri, new Parameters(), new Headers());
    }

    public Action put(final String uri, final String body, final Parameters parameters, final Headers headers) {
        return new Action(this, new Request("PUT", uri, body, parameters, headers));
    }

    public Action put(final String uri, final String body, final Parameters parameters) {
        return put(uri, body, parameters, new Headers());
    }

    public Action put(final String uri, final String body, final Headers headers) {
        return put(uri, body, new Parameters(), headers);
    }

    public Action put(final String uri, final String body) {
        return put(uri, body, new Parameters(), new Headers());
    }

    public Action post(final String uri, final String body, final Parameters parameters, final Headers headers) {
        return new Action(this, new Request("POST", uri, body, parameters, headers));
    }

    public Action post(final String uri, final String body, final Parameters parameters) {
        return post(uri, body, parameters, new Headers());
    }

    public Action post(final String uri, final String body, final Headers headers) {
        return post(uri, body, new Parameters(), headers);
    }

    public Action post(final String uri, final String body) {
        return post(uri, body, new Parameters(), new Headers());
    }

    void verify() {
        if (!this.primedInvocations.isEmpty()) {
            throw new IllegalStateException("Primed requests not invoked: " + this.primedInvocations);
        }
    }

    void prime(final PrimedInvocation primedInvocation) {
        this.primedInvocations.add(primedInvocation);
    }

    class PrimedHandler extends AbstractHandler {

        private final String contextPath;
        private final RequestParser requestParser;
        private final ResponseHandler responseHandler;
        private final RequestMatcher requestMatcher;

        public PrimedHandler(final String contextPath) {
            this(
                    contextPath,
                    new ResponseHandler(),
                    new RequestParser(),
                    new RequestMatcher(contextPath));
        }

        public PrimedHandler(
                final String contextPath,
                final ResponseHandler responseHandler,
                final RequestParser requestParser,
                final RequestMatcher requestMatcher) {
            this.contextPath = contextPath;
            this.responseHandler = responseHandler;
            this.requestParser = requestParser;
            this.requestMatcher = requestMatcher;
        }

        @Override
        public void handle(
                final String target,
                final org.eclipse.jetty.server.Request baseRequest,
                final HttpServletRequest httpServletRequest,
                final HttpServletResponse httpServletResponse) throws IOException, ServletException {

            final String requestMethod = requestParser.parseRequestMethod(httpServletRequest);
            final String requestUri = requestParser.parseRequestUri(httpServletRequest);
            final String requestBody = requestParser.parseRequestBody(httpServletRequest);
            final Map<String,String> requestHeaders = requestParser.parseHeaders(httpServletRequest);
            final Map<String,String> requestParameters = requestParser.parseRequestParameters(httpServletRequest);

            boolean handled = false;

            if (requestUri.startsWith(contextPath)) {

                for (final PrimedInvocation primedInvocation : primedInvocations) {

                    if (requestMatcher.matchRequestMethod(requestMethod, primedInvocation) &&
                        requestMatcher.matchRequestUri(requestUri, primedInvocation) &&
                        requestMatcher.matchRequestBody(requestBody, primedInvocation) &&
                        requestMatcher.matchHeaders(requestHeaders, primedInvocation) &&
                        requestMatcher.matchRequestParameters(requestParameters, primedInvocation)) {

                        final Response response = primedInvocation.getResponses().remove(0);
                        if (primedInvocation.getResponses().isEmpty()) {
                            primedInvocations.remove(primedInvocation);
                        }
                        responseHandler.respond(response, httpServletResponse);
                        handled = true;
                        break;
                    }

                }

            }

            if (!handled) {
                responseHandler.respond(INVALID_REQUEST, httpServletResponse);
            }

        }

    }

}