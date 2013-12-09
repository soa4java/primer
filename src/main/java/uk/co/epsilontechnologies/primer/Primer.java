package uk.co.epsilontechnologies.primer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.epsilontechnologies.primer.domain.*;
import uk.co.epsilontechnologies.primer.matcher.RequestMatcher;
import uk.co.epsilontechnologies.primer.server.PrimerServer;
import uk.co.epsilontechnologies.primer.server.RequestHandler;
import uk.co.epsilontechnologies.primer.server.ResponseHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static uk.co.epsilontechnologies.primer.domain.ResponseBuilder.response;

/**
 * A canned HTTP Server instance that can be programmed to behave in a required fashion - to return the appropriate
 * response/s when given specific request/s.
 *
 * @author Shane Gibson
 */
public class Primer {

    /**
     * Logger to use for error / warn / debug logging
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Primer.class);

    /**
     * The primed requests and corresponding responses
     */
    private final List<PrimedInvocation> primedInvocations = new ArrayList();

    /**
     * The server instance that is being primed
     */
    private final PrimerServer server;

    /**
     * Constructs a Primer instance for the given port and context path
     * @param contextPath the context path of the web application being primed
     * @param port the port of the web application being primed
     */
    public Primer(final String contextPath, final int port) {
        this.server = new PrimerServer(port, new PrimedHandler(contextPath));
    }

    /**
     * Starts the primer server instance
     */
    public void start() {
        this.server.start();
    }

    /**
     * Stops the primer server instance and clears the primed invocations
     */
    public void stop() {
        this.primedInvocations.clear();
        this.server.stop();
    }

    /**
     * Clears the primed invocations
     */
    public void reset() {
        this.primedInvocations.clear();
    }

    public PrimedRequest receives(final RequestBuilder requestBuilder) {
        return new PrimedRequest(this, requestBuilder.build());
    }

    /**
     * Verifies that all of the primed invocations were actually invoked.
     * @throws IllegalStateException at least one primed request was not invoked
     */
    void verify() {
        if (!this.primedInvocations.isEmpty()) {
            LOGGER.error("PRIMER --- Primed Requests Not Invoked. [PrimedInvocations:" + this.primedInvocations + "]");
            throw new IllegalStateException("Primed Requests Not Invoked");
        }
    }

    /**
     * Primes this instance with the given request and responses
     * @param request the request to prime
     * @param responses the responses to prime
     * @responses the responses to prime
     */
    void prime(final Request request, final Response... responses) {
        for (final PrimedInvocation primedInvocation : primedInvocations) {
            if (primedInvocation.getRequest().equals(request)) {
                for (final Response response : responses) {
                    primedInvocation.getResponses().add(response);
                }
                return;
            }
        }
        primedInvocations.add(new PrimedInvocation(request, responses));
    }

    /**
     * Handler implementation for each HTTP request
     */
    class PrimedHandler implements RequestHandler {

        /**
         * Handler for the response
         */
        private final ResponseHandler responseHandler;

        /**
         * Matcher for checking whether the request matches a primed request
         */
        private final RequestMatcher requestMatcher;

        /**
         * Constructs the primed handler for the given context path
         * @param contextPath the context path of the request being handled
         */
        public PrimedHandler(final String contextPath) {
            this(new ResponseHandler(), new RequestMatcher(contextPath));
        }

        /**
         * Constructs the primed handler for the given context path
         * @param responseHandler the response handler to use
         * @param requestMatcher the request matcher to use
         */
        private PrimedHandler(final ResponseHandler responseHandler, final RequestMatcher requestMatcher) {
            this.responseHandler = responseHandler;
            this.requestMatcher = requestMatcher;
        }

        /**
         * Handles the given HTTP Servlet Request and HTTP Servlet Response.
         * Checks whether the given http servlet request matches any of the primed invocations.
         * If so, the corresponding response is issued, otherwise a 404 response is issued.
         * @param httpServletRequest the HTTP servlet request that has been issued
         * @param httpServletResponse the HTTP servlet response being returned
         */
        @Override
        public void handle(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {

            final HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(httpServletRequest);

            if (!checkPrimedInvocations(new ArrayList(primedInvocations), requestWrapper, httpServletResponse)) {
                LOGGER.error("PRIMER :-- Request Not Primed. [PrimedInvocations:" + primedInvocations + "]");
                this.responseHandler.respond(response(404).withContentType("text/plain").withBody("Request Not Primed").build(), httpServletResponse);
            }

        }

        /**
         * Recursive function to check whether the given request matches one of the primed invocations
         * If a match is found, the corresponding response is issued
         * @param primedInvocationsToCheck the primed invocations to check
         * @param requestWrapper the request wrapper
         * @param httpServletResponse the HTTP servlet response
         * @return true if one of the primed invocations match the request, false otherwise
         */
        private boolean checkPrimedInvocations(
                final List<PrimedInvocation> primedInvocationsToCheck,
                final HttpServletRequestWrapper requestWrapper,
                final HttpServletResponse httpServletResponse) {

            if (!primedInvocationsToCheck.isEmpty()) {

                final PrimedInvocation primedInvocationToCheck = primedInvocationsToCheck.remove(0);

                if (this.requestMatcher.match(primedInvocationToCheck.getRequest(), requestWrapper)) {

                    final Response response = primedInvocationToCheck.getResponses().remove(0);

                    if (primedInvocationToCheck.getResponses().isEmpty()) {
                        primedInvocations.remove(primedInvocationToCheck);
                    }

                    this.responseHandler.respond(response, httpServletResponse);

                    return true;
                }

                return checkPrimedInvocations(primedInvocationsToCheck, requestWrapper, httpServletResponse);

            }

            return false;
        }

    }

}