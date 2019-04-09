package signin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.logging.Logger;

public class SocketThread implements Runnable {
	
	private Socket socket;
	private Connection connection;
	private ResultSet resultSet;
	
	public SocketThread(Socket socket){
		this.socket=socket;
	}
	
    public void run() {
		try{
			connection=DBUtils.connect();
			DataInputStream inputStream=new DataInputStream(socket.getInputStream());
			DataOutputStream outputStream=new DataOutputStream(socket.getOutputStream());
			String cmdkind=inputStream.readUTF();
			
			if(cmdkind.equals("teacherRegister")){
				String name=inputStream.readUTF();
				String psw=inputStream.readUTF();
				String yesAnswer="succed";
				String noAnswer="fail";
				//get the maxest id num;
				int num=0;
				String numsql="select max(num) from teacherReg";
				resultSet=DBUtils.select(connection, numsql);
				if(resultSet.next()){
					num=resultSet.getInt(1);
				}
				//the num increases by one
				num++;
				String sql="insert into teacherReg values("+num+",'"+name+"','"+psw+"')";
				int result=DBUtils.insert(connection, sql);
				if(result==-1){
					outputStream.writeUTF(noAnswer);
				}
				else{
					outputStream.writeUTF(yesAnswer);
					outputStream.writeInt(num);
				}
			}
			else if(cmdkind.equals("studentRegister")){
				String name=inputStream.readUTF();
				String psw=inputStream.readUTF();
				String stuID=inputStream.readUTF();
				String stuMajor=inputStream.readUTF();
				String yesAnswer="succed";
				String noAnswer="fail";
				
				int num=0;
				String numsql="select max(num) from studentReg";
				resultSet=DBUtils.select(connection, numsql);
				if(resultSet.next()){
					num=resultSet.getInt(1);
				}
				//the num increases by one
				num++;
				//create the insert sql
				String sql="insert into studentReg values("+num+",'"+name+"','"+psw+"','"+stuID+"','"+stuMajor+"')";
				int result=DBUtils.insert(connection, sql);
				if(result==-1){
					outputStream.writeUTF(noAnswer);
				}
				else{
					outputStream.writeUTF(yesAnswer);
					outputStream.writeInt(num);
				}
			}
			else if(cmdkind.equals("setClass")){
				String className=inputStream.readUTF();
				int teacherNum=inputStream.readInt();
				int timeLimit=inputStream.readInt();
				
				//search if the class name has been used
				String checksql="select * from classInfo where name='"+className+"'";
				resultSet=	DBUtils.select(connection, checksql);
				if(resultSet.next()){
					//the class name already exists
					outputStream.writeInt(-2);
					outputStream.close();
					inputStream.close();
					socket.close();
					return;
				}
				
				int num=0;
				String numsql="select max(num) from classInfo";
				resultSet=DBUtils.select(connection, numsql);
				if(resultSet.next()){
					num=resultSet.getInt(1);
				}
				num++;
				//create class sql
				String sql="insert into classInfo values("+num+",'"+className+"',"+timeLimit+","+teacherNum+",0,0,0)";
				int result=DBUtils.insert(connection, sql);
				if(result==-1){
					outputStream.writeInt(-1);
				}
				else{
					//create a signin info table for this class 
					String classTable="create table "+className+"课堂签到信息(stuNum int,siginTime nvarchar(20))";
					int createClassTable=DBUtils.createTable(connection,classTable);
					
					//create a talbe to show students who add this class
					String addClassStus="create table "+className+"课堂学生信息(stuName nvarchar(10),stuId nvarchar(20),stuMajor nvarchar(40))";
					int createAddClassStus=DBUtils.createTable(connection, addClassStus);
					
					if(createClassTable==-1 ||createAddClassStus==-1){
						//delete this class info from the classInfo table
						String deleteSql="delete from classInfo where name='"+className+"'";
						int deleteResult=DBUtils.update(connection, deleteSql);
						outputStream.writeInt(-1);
					}
					else{
						//successfully created
						outputStream.writeInt(1);
						//return the class number
						outputStream.writeInt(num);
					}
				}
			}
			else if(cmdkind.equals("addClass")){
				String className=inputStream.readUTF();
				String checkClass="select * from classInfo where name='"+className+"'";
				resultSet=DBUtils.select(connection, checkClass);
				if(!resultSet.next()){
					outputStream.writeInt(-5);
					return;
				}
				int classNum=resultSet.getInt(1);
				String name=inputStream.readUTF();
				String stuid=inputStream.readUTF();
				String major=inputStream.readUTF();
				
				String insertSql="insert into "+className+"课堂学生信息 values('"+name+"','"+stuid+"','"+major+"')";
				int result=DBUtils.insert(connection, insertSql);
				if(result>0)
					outputStream.writeInt(1);
				else
					outputStream.writeInt(0);
				
			}
			else if(cmdkind.equals("startSignin")){
				String name=inputStream.readUTF();
				String opensql="update classInfo set isOpen=1 where name='"+name+"'";
				String closesql="update classInfo set isOpen=0 where name='"+name+"'";
				String getTimeLimit="select * from classInfo where name='"+name+"'";
				int timeLimit=100;
				resultSet=DBUtils.select(connection, getTimeLimit);
				if(resultSet.next()){
					timeLimit=resultSet.getInt(3);
				}
				
				//update the signin location
				double latitude=inputStream.readDouble();
				double longitude=inputStream.readDouble();
				String updateLatitude="update classInfo set latitude="+latitude+" where name='"+name+"'";
				String updateLongitude="update classInfo set longitude="+longitude+" where name='"+name+"'";
				int updatela=DBUtils.update(connection, updateLatitude);
				int updatelo=DBUtils.update(connection, updateLongitude);
				if(updatela<0 || updatelo<0){
					System.out.println("update class location failed");
				}
				
				int openresult=DBUtils.update(connection, opensql);
				if(openresult>0){
					outputStream.writeInt(1);
					//set the class open for timeLimit
					Thread.sleep(timeLimit*60*1000);
					//close signin after timeLimit
				    DBUtils.update(connection, closesql);
				}
				else{
					outputStream.writeInt(-2);
				}
			}
			else if(cmdkind.equals("signin")){
				int classOpen=0;
				double classLatitude=0;
				double classLongitude=0;
				String className=inputStream.readUTF();
				String checkClass="select * from classInfo where name='"+className+"'";
				resultSet=DBUtils.select(connection, checkClass);
				if(resultSet.next()){
					classOpen=resultSet.getInt("isOpen");
					classLatitude=resultSet.getDouble("latitude");
					classLongitude=resultSet.getDouble("longitude");
				}
				if(classOpen==1){
					//check the location
					double userLatitude=inputStream.readDouble();
					double userLongitude=inputStream.readDouble();
					if(!DistanceUtils.isBetweenDistance(classLongitude, classLatitude, userLongitude, userLatitude)){
						outputStream.writeInt(-3);
						return;
					}
					
					int stuNum=inputStream.readInt();
					String time=inputStream.readUTF();
					
					String insertSignin="insert into "+className+"课堂签到信息 values("+stuNum+",'"+time+"')";
					int result=DBUtils.insert(connection, insertSignin);
					if(result>0){
						outputStream.writeInt(1);
					}
					else{
						outputStream.writeInt(-4);
					}
				}
				else{
					//the class is not open
					outputStream.writeInt(-2);
				}
			}
			inputStream.close();
			outputStream.close();
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

}
