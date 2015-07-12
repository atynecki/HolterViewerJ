package mvc;

import java.awt.event.*;

import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;

public class AppController implements ActionListener, SerialPortEventListener {
	private AppModel cModel = null;
	private static AppView cView = null;
	
	public static AppView getcView() {
		return cView;
	}

	private static final int STREAM_DATA_CMD = 1;
	private static final int SAVE_DATA_CMD = 2;
	private static final int DOWNLOAD_DATA_CMD = 3;
	private static final int ERASE_DATA_CMD = 4;
	private static final int GET_STATE_CMD = 6;
	
	/** parameterized constructors */
	public AppController (AppModel model, AppView view){
		this.cModel = model;
		AppController.cView = view;
		this.cModel.setController(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		Object actionSource = e.getSource();
		
		switch(actionCommand){
			case "Close":
				if(actionSource == cView.getAppViewerButtonClose())
					cView.getAppViewerFrame().dispose();
				else
					System.exit(0);
				break;
			
			case "About":
				cView.setAboutFrame();
				break;
			case "Viewer":
				cView.setViewerFrame();
				break;
			case "Clear":
				cView.cleanPatientView();
				break;
			
			case "Scan":
				cView.setPortNames(cModel.scan());
				break;
			case "Open Port":
				cModel.open(cView.getUserPort());
				cView.openPortAction();
				cModel.sendCommands(GET_STATE_CMD, 1);
				break;
			case "Close Port":
				cModel.close();
				cView.closePortAction();
				break;
			
			case "Stream data":
				if(cView.getAppButtonDataStream().isSelected() == true)
					cModel.sendCommands(STREAM_DATA_CMD, 1);	
				else
					cModel.sendCommands(STREAM_DATA_CMD, 0);
				break;
				
			case "Save data":
				if(cView.getAppButtonDataSave().isSelected() == true)
					cModel.sendCommands(SAVE_DATA_CMD, 1);
				else
					cModel.sendCommands(SAVE_DATA_CMD, 0);
				break;
				
			case "Download":
				if(cView.getAppButtonDataLoad().isSelected() == true){
					try{
					cModel.createResultFile(cView.getFileName());
					//cModel.setPatient(cView.readPatientView());
					//cModel.writePatientdDataToFile();
					} catch (AppException exception){
						exception.show_exception(); 
					}
					cModel.setDownloadDataFlag(1);
					cModel.sendCommands(DOWNLOAD_DATA_CMD, 1);
				}
				else {
					cModel.setDownloadDataFlag(0);
					cModel.closeFile();
				}
				
				break;
			case "Erase data":
				cModel.sendCommands(ERASE_DATA_CMD, 1);
				break;
			case "Time send":
				cModel.sendTime();
				break;
			case "Get state":
				cModel.sendCommands(GET_STATE_CMD, 1);
				break;
			case "Open file":
				cView.getSelectedPath();
				break;
		}
	}

	@Override
	public void serialEvent(SerialPortEvent e) {
		if (e.isRXCHAR()) {
			cModel.readBytes();
			if(cModel.getDataReadyFlag() == 1){
				cView.setDateView(cModel.getExam_time());
				cView.setTimeView(cModel.getExam_time());
			}
			else if(cModel.getDataReadyFlag() == 2){
				cView.setTimeView(cModel.calculate_current_time());
			}
			
			if(cModel.getDownloadDataFlag() == 1)
				cModel.writeDataToFile();
			else if(cModel.getDownloadDataFlag() == 2){
				cModel.setDownloadDataFlag(0);
				cModel.closeFile();
				cView.set_download_state(false);
			}
			
			if(cModel.isGetStateFlag() == true){
				cModel.setGetStateFlag(false);
				cView.set_stream_state(cModel.getDevice_state()[0]);
				cView.set_run_state(cModel.getDevice_state()[1]);
				cView.set_save_state(cModel.getDevice_state()[2]);
			}
		}
		
	}
}
