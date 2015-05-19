package mvc;

import java.util.*;
import data.*;
import adapters.*;
import jssc.SerialPortException;

public class AppModel {
	private AppController controller_;
	private COMPortAdapter PortCOM;
	static private int portComBaudrate = 115200;
	private FileAdapter resultFile;
	
	/** default constructors */
	public AppModel (){
		controller_ = null;
		PortCOM =  new COMPortAdapter();
		//resultFile = new FileAdapter();
	}
	
	public void setController(AppController c){
		this.controller_ = c;
	}
	
	public void open(String portName){
		try {
			PortCOM.connect(portName, portComBaudrate, 8, 1, 0);
			PortCOM.startPortListening(this.controller_);
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
}
