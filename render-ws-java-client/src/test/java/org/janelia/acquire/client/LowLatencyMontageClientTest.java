package org.janelia.acquire.client;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.janelia.alignment.spec.TileSpec;
import org.janelia.alignment.spec.stack.StackId;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.mockserver.socket.PortFactory;

import static org.mockserver.model.JsonBody.json;

/**
 * Tests the {@link LowLatencyMontageClient} class.
 *
 * @author Eric Trautman
 */
public class LowLatencyMontageClientTest {

    private static int mockServerPort;
    private static ClientAndServer mockServer;
    private static final File montageWorkDirectory = new File("test-montage").getAbsoluteFile();

    private final StackId acquireStackId = new StackId("tester", "testProject", "testAcquire");

    @BeforeClass
    public static void before() throws Exception {
        mockServerPort = PortFactory.findFreePort();
        mockServer = ClientAndServer.startClientAndServer(mockServerPort);
    }

    @AfterClass
    public static void after() throws Exception {
        mockServer.stop();
        deleteMontageWorkDirectory();
    }

    private static void deleteMontageWorkDirectory()
            throws IOException {
        FileUtils.deleteDirectory(montageWorkDirectory);
    }

    @Test
    public void testClient() throws Exception {

        mockServer.reset();
        deleteMontageWorkDirectory();

        for (int i = 0; i < 5; i++) {
            addAcqNextTileResponse(getAcquisitionTile(AcquisitionTile.ResultType.TILE_FOUND, "tile_" + i, 1.0));
        }

        addAcqNextTileResponse(getAcquisitionTile(AcquisitionTile.ResultType.SERVED_ALL_ACQ, null, null));

        addRenderResolvedTilesResponse();

        addAcqTileStateResponse();

        final String montageScript;
        if (File.separatorChar == '/') {
            montageScript = "ls";
        } else {
            montageScript = "dir";
        }

        LowLatencyMontageClient.main(new String[] {
                "--baseDataUrl", getBaseDataUrl(),
                "--owner", acquireStackId.getOwner(),
                "--project", acquireStackId.getProject(),
                "--stack", acquireStackId.getStack(),
                "--baseAcquisitionUrl", getBaseAcquisitionUrl(),
                "--montageStack", "testMontage",
                "--montageScript", montageScript,
                "--montageParametersFile", "src/test/resources/montage-test/montage-parameters.json",
                "--montageWorkDirectory", montageWorkDirectory.getAbsolutePath()
        });


        Assert.assertTrue("test failed", mockServer.isRunning());

    }

    private String getBaseDataPath() {
        return "/render-ws/v1";
    }

    private String getBaseDataUrl() {
        return "http://localhost:" + mockServerPort + getBaseDataPath();
    }

    private String getBaseAcquisitionPath() {
        return "/service/v1";
    }

    private String getBaseAcquisitionUrl() {
        return "http://localhost:" + mockServerPort + getBaseAcquisitionPath();
    }

    private AcquisitionTile getAcquisitionTile(final AcquisitionTile.ResultType resultType,
                                               final String tileId,
                                               final Double z) {
        String section = null;
        if (z != null) {
            section = z.toString();
        }
        TileSpec tileSpec = null;
        if (tileId != null) {
            tileSpec = TileSpec.fromJson(TILE_SPEC_JSON);
            tileSpec.setTileId(tileId);
            tileSpec.setZ(z);
        }
        return new AcquisitionTile("ACQ-1", resultType, section, tileSpec);
    }

    private void addRenderResolvedTilesResponse() {
        final String requestPath = getBaseDataPath() + "/owner/" + acquireStackId.getOwner() + "/project/" +
                                   acquireStackId.getProject() + "/stack/" + acquireStackId.getStack() + "/resolvedTiles";
        mockServer
                .when(
                        HttpRequest.request()
                                .withMethod("PUT")
                                .withPath(requestPath),
                        Times.once()
                )
                .respond(
                        HttpResponse.response()
                                .withStatusCode(HttpStatus.SC_CREATED)
        );

    }

    private void addAcqNextTileResponse(final AcquisitionTile acquisitionTile) {

        final JsonBody responseBody = json(acquisitionTile.toJson());

        mockServer
                .when(
                        HttpRequest.request()
                                .withMethod("POST")
                                .withPath(getBaseAcquisitionPath() + "/next-tile"),
                        Times.once()
                )
                .respond(
                        HttpResponse.response()
                                .withStatusCode(HttpStatus.SC_OK)
                                .withHeader("Content-Type", responseBody.getContentType())
                                .withBody(responseBody)
        );
    }

    private void addAcqTileStateResponse() {
        mockServer
                .when(
                        HttpRequest.request()
                                .withMethod("PUT")
                                .withPath(getBaseAcquisitionPath() + "/tile-state"),
                        Times.once()
                )
                .respond(
                        HttpResponse.response()
                        .withStatusCode(HttpStatus.SC_OK)
        );

    }

    private static final String TILE_SPEC_JSON =
            "{\n" +
            "  \"width\": 2650.0,\n" +
            "  \"height\": 2260.0,\n" +
            "  \"mipmapLevels\": {\n" +
            "    \"0\": {\n" +
            "      \"imageUrl\": \"src/test/resources/stitch-test/col0075_row0021_cam1.png\",\n" +
            "      \"maskUrl\": \"src/test/resources/stitch-test/test_mask.jpg\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"transforms\": {\n" +
            "    \"type\": \"list\",\n" +
            "    \"specList\": [\n" +
            "      {\n" +
            "        \"className\": \"mpicbg.trakem2.transform.AffineModel2D\",\n" +
            "        \"dataString\": \"0.959851    -0.007319      0.00872     0.923958      47.5933      45.6929\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"className\": \"mpicbg.trakem2.transform.AffineModel2D\",\n" +
            "        \"dataString\": \"1  0  0  1  0  0\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";


}