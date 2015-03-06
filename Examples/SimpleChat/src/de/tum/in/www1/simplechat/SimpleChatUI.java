package de.tum.in.www1.simplechat;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Label;

public class SimpleChatUI {
	private LocalChatPeer chatPeer;
	List chatPeerList;
	StyledText chatText;
	ProgressBar progressBar;
	
	protected Shell shlSimpleChatExample;
	private Text txtDisplayName;
	private ChatRoom selectedPeer;
	private ArrayList<ChatRoom> chatPeers = new ArrayList<>();
	
	private Executor swtExecutor = new Executor() {
		@Override
		public synchronized void execute(Runnable command) {
			Display.getDefault().syncExec(command);
		}
	};
	private Text txtMessage;
	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			SimpleChatUI window = new SimpleChatUI();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		chatPeer = new LocalChatPeer(SimpleChatUI.this, swtExecutor);

		shlSimpleChatExample.open();
		shlSimpleChatExample.layout();
		while (!shlSimpleChatExample.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	public void startLocalPeer() {
		chatPeer.start(txtDisplayName.getText());
	}

	public void addChatPeer(ChatRoom chatPeer) {
		this.chatPeers.add(chatPeer);
		this.chatPeerList.add(chatPeer.getDisplayName());
	}
	public void removeChatPeer(ChatRoom chatPeer) {
		this.chatPeers.remove(chatPeer);
		this.chatPeerList.removeAll();
		
		for (ChatRoom peer : this.chatPeers) {
			this.chatPeerList.add(peer.getDisplayName());
		}
	}
	
	
	public void selectChatPeer(int index) {
		if (index == -1 || index >= this.chatPeers.size()) {
			this.selectedPeer = null;
		} else {
			this.selectedPeer = this.chatPeers.get(index);
			this.updateChatData();
		}
	}
	
	public void updateChatData() {
		if (this.selectedPeer == null) return;
		
		this.chatText.setText(this.selectedPeer.getChatText());
		this.progressBar.setSelection(this.selectedPeer.getFileProgress());
	}
	
	public FileChannel getSaveFileChannel(String fileName) {		
		FileDialog fd = new FileDialog(shlSimpleChatExample, SWT.SAVE);
        fd.setText("Choose a file for the incoming file transfer");
        fd.setFileName(fileName);
        String selected = fd.open();
        Path path = FileSystems.getDefault().getPath(selected);
        OpenOption[] read = { StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW };
        try {
        	System.out.println("File will be saved to: "+path);
			FileChannel fileChannel = FileChannel.open(path, read);

			return fileChannel;
        } catch (IOException e) {
			e.printStackTrace();
		}
        
        return null;
	}
	
	public void sendFile() {
		if (this.selectedPeer == null) return;
		
		FileDialog fd = new FileDialog(shlSimpleChatExample, SWT.OPEN);
        fd.setText("Choose a file to send");
        String selected = fd.open();

        if (selected == null) return;
        
        Path path = FileSystems.getDefault().getPath(selected);
        OpenOption[] read = { StandardOpenOption.READ };
        try {
			FileChannel fileChannel = FileChannel.open(path, read);
					
			this.selectedPeer.sendFile(fileChannel, path.getFileName().toString(), (int)fileChannel.size());
        } catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shlSimpleChatExample = new Shell();
		shlSimpleChatExample.setSize(621, 496);
		shlSimpleChatExample.setText("Simple Chat Example");
		shlSimpleChatExample.setLayout(null);
		
		this.chatPeerList = new List(shlSimpleChatExample, SWT.BORDER);
		chatPeerList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectChatPeer(chatPeerList.getSelectionIndex());
			}
		});
		chatPeerList.setBounds(10, 66, 243, 332);
		
		txtDisplayName = new Text(shlSimpleChatExample, SWT.BORDER);
		txtDisplayName.setText("Enter a Display Name");
		txtDisplayName.setBounds(10, 10, 148, 24);
		
		Button btnNewButton = new Button(shlSimpleChatExample, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				startLocalPeer();
			}
		});
		btnNewButton.setBounds(168, 6, 243, 34);
		btnNewButton.setText("Start Advertising and Browsing");
		
		this.chatText = new StyledText(shlSimpleChatExample, SWT.BORDER);
		chatText.setDoubleClickEnabled(false);
		chatText.setEnabled(false);
		chatText.setEditable(false);
		chatText.setBounds(259, 66, 348, 332);
		
		txtMessage = new Text(shlSimpleChatExample, SWT.BORDER);
		txtMessage.setText("Type a message");
		txtMessage.setBounds(259, 404, 269, 23);
		
		Button btnSend = new Button(shlSimpleChatExample, SWT.NONE);
		btnSend.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (selectedPeer != null) {
					selectedPeer.sendMessage(txtMessage.getText());
				}
			}
		});
		btnSend.setBounds(534, 404, 73, 24);
		btnSend.setText("Send");
		
		Button btnSendFile = new Button(shlSimpleChatExample, SWT.NONE);
		btnSendFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sendFile();
			}
		});
		btnSendFile.setBounds(259, 433, 106, 32);
		btnSendFile.setText("Send a File");
		
		this.progressBar = new ProgressBar(shlSimpleChatExample, SWT.NONE);
		progressBar.setBounds(371, 453, 236, 14);
		
		Label lblPeers = new Label(shlSimpleChatExample, SWT.NONE);
		lblPeers.setBounds(10, 46, 243, 14);
		lblPeers.setText("Discovered Peers");
		
		Label lblChat = new Label(shlSimpleChatExample, SWT.NONE);
		lblChat.setEnabled(false);
		lblChat.setBounds(259, 46, 348, 14);
		lblChat.setText("Chat with Selected Peer");
		
		Label lblFileProgress = new Label(shlSimpleChatExample, SWT.NONE);
		lblFileProgress.setBounds(368, 433, 148, 14);
		lblFileProgress.setText("File Transfer Progress");

	}
}
