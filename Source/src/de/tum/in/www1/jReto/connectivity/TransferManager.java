package de.tum.in.www1.jReto.connectivity;


public interface TransferManager {
	void cancelTransfer(InTransfer transfer);
	void cancelTransfer(OutTransfer transfer);
}
