import org.apache.airavata.integration.AiravataClientFactory;
import org.apache.airavata.integration.clients.AiravataAPIClient;
import org.apache.airavata.integration.clients.IdentityManagementClient;
import org.apache.airavata.integration.utils.Constants;
import org.apache.airavata.integration.utils.SFTPFileHandler;
import org.apache.airavata.model.security.AuthzToken;
import org.apache.airavata.model.status.ExperimentStatus;
import org.json.JSONObject;

import java.io.File;

public class TestExperiments {

    private static String filePath = "config.properties file path";

    public static void main(String[] args) throws Exception {

        IdentityManagementClient identityManagementClient = AiravataClientFactory.
                getIdentityManagementClient(filePath);

        System.out.println("Obtaning access token .....");
        JSONObject object = identityManagementClient.
                getAccessToken("CHANGE_ME", "CHANGE_ME");

        String access_token = object.get("access_token").toString();
        String refresh_token = object.get("refresh_token").toString();

        AuthzToken authzToken = identityManagementClient.getAuthToken(access_token);

//        // to obtain from refresh token
//        JSONObject jsonObject =  identityManagementClient.getTokenFromRefreshToken(refresh_token);
//         access_token = object.get("access_token").toString();
//        System.out.println("ACR "+ access_token);
//        //logout
//        identityManagementClient.logout(refresh_token);

        String username = authzToken.getClaimsMap().get(Constants.USER_NAME);

        System.out.println("successfully authenticated: username " + username);
        AiravataAPIClient airavataAPIClient = AiravataClientFactory.getAiravataAPIClient(filePath);
        SFTPFileHandler fileHandler = AiravataClientFactory.getSFTPHandler(filePath);

        System.out.println("Uploading input files ......");
        String path = fileHandler.uploadFile(username, authzToken.getAccessToken(),
                "CHANGE_ME");


        String remoteFullPath =
                fileHandler.getProperties().getProperty(Constants.REMOTE_FILE_SYSTEM_ROOT) + File.separator + username + path + File.separator;

        System.out.println("Creating experiment .....");
        String experimentId = airavataAPIClient.createExperiment(authzToken, "snowapp_client_test_a", "testing sample",
                remoteFullPath, "SCAN000702.xyz", remoteFullPath);

        System.out.println("Experiment Id " + experimentId);

        System.out.println("Launching experiment .....");
        airavataAPIClient.launchExperiment(authzToken, experimentId);

        System.out.println("Fetching experiment status .....");
        ExperimentStatus status = airavataAPIClient.getExperimentStatus(authzToken, experimentId);

        while (status.getState().getValue() < 4) {

            if (status.getReason() != null) {
                System.out.println("Experiment status " + status.getReason());
            }
            Thread.sleep(30000);
            status = airavataAPIClient.getExperimentStatus(authzToken, experimentId);
        }
        System.out.println("Experiment status " + status.getReason());

         fileHandler.downloadFiles(username, authzToken.getAccessToken(), path,"CHANGE_ME");

    }
}
