package me.playground.robotsense;

import java.lang.String;
import java.lang.Runnable;
import java.lang.Thread;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.KeyManagementException;
import java.net.SocketException;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import java.net.Socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;

import java.security.cert.X509Certificate;
import java.io.FileInputStream;
import java.io.BufferedInputStream;

import android.os.Handler;

import android.util.Log;

public class NetLink implements Runnable {
	private String TAG = "NetLink";
	private SSLContext context = null;
	private DataInputStream in = null;
	private DataOutputStream out;
	private final int port = 5678;
	private final String ip_address = "192.168.0.100";
	private ControlResponse newCR;
	private boolean sent = false;
	private int oldAngle, newAngle;
	private int power, oldPower;
	private int turn, oldTurn;

	private void receive() {
		int recv = 8;

		if (in != null && sent)
			try {
				Log.i(TAG, "wait for data");

				byte bData[] = new byte[recv];
				recv = in.read(bData);
				if (recv < 8) {
					Log.i(TAG, "received " + recv);
				} else {
					ByteBuffer b = ByteBuffer.allocate(recv);
					b.put(bData);
					b.rewind();
					IntBuffer intBuffer = b.asIntBuffer();
					newCR.sonarAngle = intBuffer.get(0);
					newCR.distance = intBuffer.get(1);
					Log.i(TAG, "received {" + newCR.sonarAngle +
					      ", " + newCR.distance + "}");
				}
			} catch(IOException iox) {
				Log.e(TAG, "IOException: read()");
				return;
			}
	}

	public void getCurrent(ControlResponse cr) {
		receive();
		cr.sonarAngle = newCR.sonarAngle;
		cr.distance = newCR.distance;
	}

	public void update(ControlResponse cr) {
		newAngle = cr.sonarAngle;
		power = cr.advance;
		turn = cr.turn;

		Log.i(TAG, "param: " + cr.sonarAngle + ", " +
		      cr.advance + ", " + cr.turn);
	}

	public NetLink() {
		newCR = new ControlResponse();
		newAngle = 0;

		// Load CAs from an InputStream
		// (could be from a resource or ByteArrayInputStream or ...)
		CertificateFactory cf;
		try {
			cf = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			Log.e(TAG, "CertificateException: get()");
			return;
		}

		// From https://www.washington.edu/itconnect/security/ca/load-der.crt
		InputStream caInput;
		try {
			caInput = new BufferedInputStream(new FileInputStream("/storage/emulated/0/Android/data/me.playground.robotsense/files/robot.crt"));
		} catch (FileNotFoundException e) {
			Log.e(TAG, "FileNotFoundException");
			return;
		}

		Certificate ca;
		try {
			ca = cf.generateCertificate(caInput);
			Log.d(TAG, "ca=" + ((X509Certificate) ca).getSubjectDN());
		} catch (CertificateException e) {
			Log.e(TAG, "CertificateException: generate()");
			return;
		} finally {
			try {
				caInput.close();
			} catch (IOException e) {
				Log.e(TAG, "IOException: close()");
				return;
			}
		}

		// Create a KeyStore containing our trusted CAs
		String keyStoreType = KeyStore.getDefaultType();
		KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance(keyStoreType);
		} catch (KeyStoreException e) {
			Log.e(TAG, "KeyStoreException");
			return;
		}
		try {
			keyStore.load(null, null);
		} catch (IOException e) {
			Log.e(TAG, "keystore: IOException");
			return;
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "NoSuchAlgorithmException");
			return;
		} catch (CertificateException e) {
			Log.e(TAG, "CertificateException");
			return;
		}
		try {
			keyStore.setCertificateEntry("ca", ca);
		} catch (KeyStoreException e) {
			Log.e(TAG, "KeyStoreException");
			return;
		}

		// Create a TrustManager that trusts the CAs in our KeyStore
		String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory tmf;
		try {
			tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "NoSuchAlgorithmException");
			return;
		}
		try {
			tmf.init(keyStore);
		} catch (KeyStoreException e) {
			Log.e(TAG, "KeyStoreException");
			return;
		}

		// Create an SSLContext that uses our TrustManager
		try {
			context = SSLContext.getInstance("TLS");
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "NoSuchAlgorithmException");
			return;
		}
		try {
			context.init(null, tmf.getTrustManagers(), null);
		} catch (KeyManagementException e) {
			Log.e(TAG, "KeyManagementException");
			return;
		}
	}

	public int connect() {
		SSLSocket sock;

		if (context == null)
			return -1;

		try {
			sock = (SSLSocket)context.getSocketFactory().createSocket(
				ip_address, port);
		} catch (IOException e) {
			Log.e(TAG, "sock: IOException");
			return -1;
		}

		try {
			sock.setKeepAlive(true);
		} catch (SocketException e) {
			Log.e(TAG, "SocketException");
			return -1;
		}
		try {
			sock.startHandshake();

			in = new DataInputStream(sock.getInputStream());
			out = new DataOutputStream(sock.getOutputStream());
		} catch (IOException e) {
			Log.e(TAG, "handshake: IOException");
			return -1;
		}

		return 0;
	}

	public void send(int x[]) {
		ByteBuffer b = ByteBuffer.allocate(4 * x.length);
		IntBuffer intBuffer = b.asIntBuffer();
		intBuffer.put(x);
		// the initial order of a byte buffer is always BIG_ENDIAN.

		byte result[] = b.array();

		Log.i(TAG, "send " + x[0] + ", " + x[1]);

		try {
			out.write(result, 0, result.length);
		} catch (IOException e) {
			Log.e(TAG, "send: IOException");
			return;
		}
	}

	@Override
	public void run() {
		if (connect() == 0) {
			// This will be replaced with a joystick control
			oldAngle = newAngle + 1;
			turn = 0;
			power = 0;
			for (;;) {
				if (newAngle != oldAngle |
				    power != oldPower ||
				    turn != oldTurn) {
					oldAngle = newAngle;
					int x[] = {oldAngle, power, turn};
					send(x);
					sent = true;
				}

				/* milliseconds */
				try {
					Thread.sleep(100);
				} catch (Exception ignore) {}
			}
		}
	}
}
