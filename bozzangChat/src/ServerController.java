import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class ServerController implements Initializable{
	
	@FXML TextField txtPort, txtNotice;
	@FXML TextArea txtArea;
	@FXML TableView<User> tableUser;
	@FXML TableColumn<User, String> tcUser, tcIP;
	@FXML Button btnConn, btnSend;
	
	ArrayList<User> userList = new ArrayList<>();
	
	static int PORT;
	ServerSocket server = null;	
	
	class ServerStart extends Thread{
		
		int portNum;
		
		public ServerStart(int portNum) {
			this.portNum = portNum;
		}
		
		@Override
		public void run() {
			try {
				server = new ServerSocket(portNum);
				
				while(true) {
					//서버가 열려서 계속 유저를 받음..
					Socket soc = server.accept();
					
					//User 객체를 생성, 쓰레드시작
					User user = new User(soc);
					userList.add(user);
					user.start();
					
					//userTable새로고침
					loadUser();
					
				}
			} catch (IOException e) {
			}
			
			
		}
		
	}
	
	public class User extends Thread{
		Socket soc;
		PrintWriter writer;
		
		private SimpleStringProperty nick = new SimpleStringProperty();
		private SimpleStringProperty ip = new SimpleStringProperty();
		


		//생성자
		public User(Socket soc) {
			this.soc = soc;
			this.ip.set(soc.getRemoteSocketAddress().toString());
			try {
				this.writer = new PrintWriter(soc.getOutputStream(),true);
			} catch (IOException e) {
			}
		}
		
		public SimpleStringProperty nickProperty() {
			return nick;
		}
		
		
		public SimpleStringProperty ipProperty() {
			return ip;
		}
		
		@Override
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(soc.getInputStream(),"UTF-8"));
				this.nick.set(reader.readLine());
				String nickName = nick.get();
				
				//접속메세지 서버와 유저들에게 출력
				String msgWelcome = nickName +" 님이 입장하였습니다.";
				
				sendAll(msgWelcome);
				
				
				//클라이언트에 있는 유저 리스트 업데이트..
				sendAll("!@#$%^!@#$%^UDATEMEMBER!@#$%^!@#$%^");
				StringBuffer addMem = new StringBuffer(); 
				for(User uu : userList) {
					addMem.append(uu.nick.get());
					addMem.append("/");
				}
				sendAll(addMem.toString());
				
				
				Platform.runLater(()->{
					txtArea.appendText(msgWelcome+"\n"); //접속할때마다 메세지 출력
				});
				//데이터를 수신 받아서 전송
				while(true) {
					String msg = reader.readLine();
					
					btnSend.setOnAction(new EventHandler<ActionEvent>() {
						
						@Override
						public void handle(ActionEvent event) {
							
							String msg = txtNotice.getText();
							sendAll("[Notice] "+msg);
							Platform.runLater(()->{
								txtArea.appendText("공지를 전달하였습니다.\n");
							});
							
						}
					});
					
					if(msg == null) {
						sendAll(nickName+" 님이 퇴장하셨습니다.");
						
						
						Platform.runLater(()->{
							txtArea.appendText(nickName+" 님이 퇴장하셨습니다.\n"); //퇴장할때마다 메세지 출력
						});
						//나가면 유저리스트에서 지운다.
						userList.remove(this);
						//클라이언트에 있는 유저 리스트 업데이트..
						sendAll("!@#$%^!@#$%^UDATEMEMBER!@#$%^!@#$%^");
						addMem = new StringBuffer(); 
						for(User uu : userList) {
							addMem.append(uu.nick.get());
							addMem.append("/");
						}
						sendAll(addMem.toString());
						/////
						break;
					}
					//나가도 null이 출력되지 않게 위치를 이쪽에
					sendAll("["+nickName+"] "+msg);
					
				}
				//테이블뷰업데이트
				loadUser();
				
			} catch (IOException e) {
			}
			
			
		}
		//전송
		public void sendAll(String msg) {
			for(User uu : userList) {
				uu.writer.println(msg);
				uu.writer.flush();
			}
			
		}
		
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		txtPort.setText("10002");
		
		tcUser.setCellValueFactory(new PropertyValueFactory<User,String>("nick"));
		tcIP.setCellValueFactory(new PropertyValueFactory<User,String>("ip"));
		
	}
	
	public void handleConn() {
		
		PORT = Integer.parseInt(txtPort.getText());
		ServerStart serverStart = new ServerStart(PORT);
		serverStart.setDaemon(true);
		serverStart.start();
		btnConn.setDisable(true);
		txtArea.appendText("서버를 시작합니다.\n");
	}
	
	
	public void loadUser() {
		tableUser.getItems().clear();
		tableUser.getItems().addAll(userList);
		txtArea.appendText("총 접속자 수 : "+ userList.size() +"명\n");
	}
	

}
