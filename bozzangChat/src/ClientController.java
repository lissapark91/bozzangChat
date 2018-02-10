import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
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

public class ClientController implements Initializable {
	
	@FXML TextField txtPort, txtIP, txtNick;
	@FXML TextArea txtArea;
	@FXML TextField txtChat;
	@FXML TableView<Users> tableUser;
	@FXML TableColumn<Users, String> tcUsers;
	@FXML Button btnSend, btnConn;
	
	Socket soc = new Socket();
	String nickName;
	String sendMsg;
	ArrayList<Users> userList = new ArrayList<>();
	ObservableList<Users> userOblist = null;
	
	SenderThread sender;
	ReceiverThread receiver;
	boolean existServer = false;
	

	
	//테이블에 데이터를 넣기위해서.. 만듬..ㅠㅠ
	public class Users{
		SimpleStringProperty usersNick = new SimpleStringProperty();
		
		public Users(String nick) {
			this.usersNick.set(nick);
		}
		public SimpleStringProperty usersNickProperty() {
			return usersNick;
		}
	}
	
	
	//스타트 쓰레드
	class UserStart extends Thread {
		String nick;
		String ip;
		int port;
		
		//생성자
		public UserStart(String nick, String ip, int port) {
			this.nick = nick;
			this.ip = ip;
			this.port = port;
		}
		
		@Override
		public void run() {
			try {
				
				soc = new Socket(ip, port);
				if(soc.isConnected()) {
					existServer=true;
				}
				existServer(existServer);
				
				
				sender = new SenderThread(soc, nick);
				receiver = new ReceiverThread(soc);
		
				sender.start();
				receiver.start();
			
			} catch (UnknownHostException e) {
			} catch (ConnectException e) {
				txtArea.appendText("서버가 존재하지 않습니다.\n");
			} catch (IOException e) {
			}
		}
	}
	
	class SenderThread extends Thread{
		
		Socket soc;
		String nick;
		
		public PrintWriter writer;
		
		public SenderThread(Socket soc, String nickName) {
			this.soc = soc;
			this.nick = nickName;
		}
		
		
		@Override
		public void run() {
			//소켓으로 전송(전송버튼이 담당하게)
			
			
			try {
				 
				 writer = new PrintWriter(soc.getOutputStream(),true);
				//닉네임을 첫번째로 쏴준다
				if(nick != null) {
					writer.println(nick);
				}
				
				
				while(true) {
					
					btnSend.setOnAction(new EventHandler<ActionEvent>() {
						
						@Override
						public void handle(ActionEvent event) {
							sendMsg = txtChat.getText();
							//업데이트 명령어 쓰지 못하게 막아버림..
							if(sendMsg.equals("!@#$%^!@#$%^UDATEMEMBER!@#$%^!@#$%^")) {
								Platform.runLater(()->{
									txtArea.appendText("[Error] 그 단어는 쓰실 수 없습니다.\n");
									txtChat.clear();
								});
								sendMsg = txtChat.getText();
							}else if(sendMsg != null && sender != null) {
								sender.writer.println(sendMsg); 
							}
							txtChat.clear();
						}
					
					});
					btnSend.setDefaultButton(true);

					
				}
				
				
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				
				try {
					if(soc != null) {
						soc.close();
						writer.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
			
		}
		
	}
	class ReceiverThread extends Thread{
		Socket soc;
		public ReceiverThread(Socket soc) {
			this.soc = soc;
		}
		
		@Override
		public void run() {
			//소켓으로부터 데이터를 수신, 텍스트 에리어에 출력..
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(soc.getInputStream()));
				
				while(true) {
					String msg = reader.readLine();
					
					if(msg == null) {
						Platform.runLater(()->{
							txtArea.appendText("서버와 연결이 끊어졌습니다. \n"); //메세지 출력
							txtChat.setDisable(true);
							btnSend.setDisable(true);
							btnConn.setDisable(false);
							txtIP.setDisable(false);
							txtNick.setDisable(false);
							txtPort.setDisable(false);
							tableUser.getItems().clear();
						});
						existServer = false;
						break;
					} else if(msg.equals("!@#$%^!@#$%^UDATEMEMBER!@#$%^!@#$%^")) {
						String addMem = reader.readLine();
						updateMem(addMem);
					} else {
						
						Platform.runLater(()->{
							txtArea.appendText(msg+"\n"); //메세지 출력
						});
					}
					
					
				}
				
				
			} catch(IOException e) {
			} finally {
				try {
					if(soc != null) soc.close();
				} catch (IOException e) {
				}
			}
		}
		
		//테이블뷰 업데이트
		public void updateMem(String msg) {
			String[] array = msg.split("/");
			userList.clear();
			for(String s : array) {
				Users us = new Users(s);
				userList.add(us);
			}
			//userOblist = FXCollections.observableArrayList(userList);
			Platform.runLater(()->{
				tableUser.getItems().clear();
				tableUser.getItems().addAll(userList);				
			});
		}
		
	}
	
	

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		txtPort.setText("10002");
		tcUsers.setCellValueFactory(new PropertyValueFactory<Users,String>("usersNick"));
		
	}
	
	public void handleConn() {
		
		nickName = txtNick.getText();
		int rand = (int) (Math.random()*10000);
		if(nickName.isEmpty()) {
			nickName = "Guest" + rand;			
		}
		
		String ip = txtIP.getText();
		int port = Integer.parseInt(txtPort.getText());
		
		if(ip==null) ip="localhost";
		UserStart userStart = new UserStart(nickName, ip, port);
		userStart.setDaemon(true);
		userStart.start();
		
		
	}
	public void existServer(boolean existServer) {
		
		if(existServer) {
			btnConn.setDisable(true);
			txtIP.setDisable(true);
			txtNick.setDisable(true);
			txtPort.setDisable(true);
			btnSend.setDisable(false);
			txtChat.setDisable(false);
		}
		
	}
	

}
