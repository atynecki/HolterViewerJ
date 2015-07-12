package mvc;

import java.time.LocalDateTime;
import java.util.Vector;

import data.*;
import adapters.*;
import parsers.*;
import jssc.SerialPortException;

public class AppModel {
	private AppController controller;
	
	private Patient patient;
	private Time exam_time;
	private Time start_exam_time;
	private Time stop_exam_time;
	private int exam_period;
	private Vector<Sample> packet_sample;
	private Boolean[] device_state = new Boolean[5];
	
	private COMPortAdapter PortCOM;
	static private int portComBaudrate = 9600;
	private byte[] comPortCommandFrame = new byte[4];
	private byte[] comPortTimeFrame = new byte[10];
	
	private FileAdapter resultFile;
	private String csvCellSeparator = ",";
	private String csvLineSeparator = System.lineSeparator();
	
	private AppDataParser appParser;
	private int dataReadyFlag;
	private int downloadDataFlag;
	private boolean getStateFlag;
	
	/** default constructors */
	public AppModel (){
		controller = null;
		patient = new Patient();
		exam_time = new Time();
		start_exam_time = new Time();
		stop_exam_time = new Time();
		exam_period = 0;
		packet_sample = new Vector<Sample>(AppDataParser.getDataSamplesNumber());
		PortCOM =  new COMPortAdapter();
		resultFile = new FileAdapter();
		appParser = new AppDataParser();
		dataReadyFlag = 0;
		downloadDataFlag = 0;
		comPortCommandFrame[0] = (byte)(0xEF);
		comPortCommandFrame[1] = (byte)(0xFE);
		comPortTimeFrame[0] = (byte)(0xEF);
		comPortTimeFrame[1] = (byte)(0xFE);
	}
	
	/** setters and getters */
	public void setController(AppController c){
		this.controller = c;
	}
	
	public void setPatient(Patient patient) {
		this.patient = patient;
	}

	public int getDataReadyFlag() {
		return dataReadyFlag;
	}
	
	public void setDataReadyFlag(int dataReadyFlag) {
		this.dataReadyFlag = dataReadyFlag;
	}

	public Time getExam_time() {
		return exam_time;
	}

	public Vector<Sample> getPacket_sample() {
		return packet_sample;
	}

	public int getDownloadDataFlag() {
		return downloadDataFlag;
	}

	public void setDownloadDataFlag(int downloadDataFlag) {
		this.downloadDataFlag = downloadDataFlag;
	}
	
	public void setStart_exam_time(Time start_exam_time) {
		this.start_exam_time = start_exam_time;
	}

	public void setStop_exam_time(Time stop_exam_time) {
		this.stop_exam_time = stop_exam_time;
	}
	
	public Boolean[] getDevice_state() {
		return device_state;
	}
	
	public boolean isGetStateFlag() {
		return getStateFlag;
	}

	public void setGetStateFlag(boolean getStateFlag) {
		this.getStateFlag = getStateFlag;
	}

	public void open(String portName){
		try {
			PortCOM.connect(portName, portComBaudrate, 8, 1, 0);
			PortCOM.startPortListening(this.controller);
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}

	public void close(){
		try {
			PortCOM.stopPortListening();
			PortCOM.disconnect();
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}
		
	public String[] scan(){
		return PortCOM.getPortsNames();
	}
	
	public boolean isConnected(){
		return PortCOM.isConnected();
	}
	
	public void clear_all_flags (){
		dataReadyFlag = 0;
		getStateFlag = false;
	}
	
	public void readBytes () {
		byte[] comPortFrame;
		comPortFrame = PortCOM.readBytesFromPort();
		appParser.parse(comPortFrame);
		this.clear_all_flags();
		
		if(appParser.getHeader_recevied() == true){
			exam_time = appParser.getTime_data();
			dataReadyFlag = 1;
		}
		else if (appParser.getSample_recevied() == true){
			packet_sample = appParser.getSample_data();
			dataReadyFlag = 2;
		}
		else if (appParser.getStart_time_received() == true){
			start_exam_time = appParser.getTime_data();
		}
		else if (appParser.getStop_time_received() == true){
			stop_exam_time = appParser.getTime_data();
			exam_period = Utils.timeDiff(stop_exam_time, start_exam_time);
		}
		else if (appParser.getState_received() == true){
			set_state(appParser.getDevice_state());
			getStateFlag = true;
		}
		else if(appParser.getTransfer_end_received() == true){
			downloadDataFlag = 2;
		}
	}
	
	public void sendCommands (int code, int value) {	
		comPortCommandFrame[2] = (byte)(code);
		comPortCommandFrame[3] = (byte)(value);
	
		try{
			PortCOM.writeBytesToPort(comPortCommandFrame);
		}catch (SerialPortException e) {
			e.printStackTrace();
		}
			
	}
	
	public void sendTime (){
		LocalDateTime localTime = LocalDateTime.now();
		comPortTimeFrame[2] = (byte)(5);
		comPortTimeFrame[3] = (byte)(localTime.getSecond());
		comPortTimeFrame[4] = (byte)(localTime.getMinute());
		comPortTimeFrame[5] = (byte)(localTime.getHour());
		comPortTimeFrame[6] = (byte)(localTime.getDayOfMonth());
		comPortTimeFrame[7] = (byte)(localTime.getMonthValue());
		comPortTimeFrame[8] = (byte)(localTime.getYear() >> 8);
		comPortTimeFrame[9] = (byte)(localTime.getYear());
		
		try{
			PortCOM.writeBytesToPort(comPortTimeFrame);
		}catch (SerialPortException e) {
			e.printStackTrace();
		}
	}
	
	public Time calculate_current_time(){
		Double min = packet_sample.get(0).getTimestamp_()%60;
		Double hour = packet_sample.get(0).getTimestamp_()/60;
		exam_time.setMinute_(min.intValue());
		exam_time.setHour_(hour.intValue());
		
		return exam_time;
	}
	
	public void createResultFile (String file_name){
		String dataDirPath = System.getProperty("user.dir") + "/" + "data" + "/";
		FileAdapter.createDirectory(dataDirPath);
		resultFile = new FileAdapter(dataDirPath + file_name + ".csv", csvCellSeparator, csvLineSeparator);
		resultFile.openOrCreateToWrite();
	}
	
	public void writePatientdDataToFile () {
		String[] p = {patient.getName_(),patient.getLast_name_(),patient.getID_num_()};
		resultFile.writeCSVLine(p);
	}
	
	public void writeDataToFile() {
		if(dataReadyFlag == 1){
			resultFile.writeLine(exam_time.toString());
		}
		else if(dataReadyFlag == 2) {
			for(Sample i:packet_sample)
				resultFile.writeLine(i.toString());
		}
	}
	
	public void closeFile() {
		resultFile.close();
	}
	
	public void set_state(int state) {
		if((state & 0x01) != 0)
			device_state[0] = true;
		else
			device_state[0] = false;
			
		if((state & 0x02) != 0)
			device_state[1] = true;
		else
			device_state[1] = false;
		
		if((state & 0x04) != 0)
			device_state[2] = true;
		else
			device_state[2] = false;
		
		if((state & 0x08) != 0)
			device_state[3] = true;
		else
			device_state[3] = false;
		
		if((state & 0x10) != 0)
			device_state[4] = true;
		else
			device_state[4] = false;
	}
	
}
