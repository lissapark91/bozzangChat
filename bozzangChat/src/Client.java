import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Client extends Application{
	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(Class.forName("Client").getResource("ClientRoot.fxml"));
		Parent root = loader.load();
		Scene scene = new Scene(root);
		
		primaryStage.setResizable(false);
		primaryStage.setScene(scene);
		primaryStage.setTitle(" == 그룹채팅 by.보짱 편하게 대화하세요. == ");
		primaryStage.show();
	
	}
	
	public static void main(String[] args) {
		launch(args);
	}

}
