package tahrir.tools;

import java.io.*;
import java.lang.reflect.Type;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;
import java.util.concurrent.*;

import net.sf.doodleproject.numerics4j.random.RandomRNG;

import org.apache.commons.codec.binary.Base64;

import tahrir.*;

import com.google.common.eventbus.EventBus;
import com.google.gson.*;

public class TrUtils {
	public static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

	public static final Random rand = new Random();

	public static final RandomRNG rng = new RandomRNG();

	public static final Runnable noopRunnable = new Runnable() {

		public void run() {
		}
	};

	public static final Gson gson;

	public static final EventBus eventBus = new EventBus();

	static {
		final GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(RSAPublicKey.class, new PublicKeyInstanceCreator());
		//builder.registerTypeAdapter(RSAPublicKey.class, new RSAPublicKeySerializer());
		gson = builder.create();
	}

	@SuppressWarnings("unchecked")
	public static <T> T parseJson(final File jsonFile, final Type type) throws JsonParseException, IOException {
		final FileReader json = new FileReader(jsonFile);
		json.close();
		return (T) gson.<Object> fromJson(json, type);
	}

	public static <T> T parseJson(final String json, final Class<T> type) throws JsonParseException {
		return gson.fromJson(json, type);
	}

	@SuppressWarnings("unchecked")
	public static <T> T parseJson(final String json, final Type type) throws JsonParseException {
		return (T) gson.<Object> fromJson(json, type);
	}

	public static File createTempDirectory() throws IOException {
		final File temp;

		temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

		if (!(temp.delete()))
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());

		if (!(temp.mkdir()))
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());

		return (temp);
	}

	public static void readAllBytes(final byte[] buffer, final DataInputStream dis) throws IOException {
		int read = 0;
		int numRead = 0;
		while (read < buffer.length && (numRead = dis.read(buffer, read, buffer.length - read)) >= 0) {
			read = read + numRead;
		}
	}

	public static RSAPublicKey getPublicKey(final String pubKeyString) {
		final byte[] bytes = Base64.decodeBase64(pubKeyString);
		try {
			return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * These methods are only for testing purposes.
	 */
	public static boolean testIsConnected(final TrNode node1, final TrNode node2) {
		return node1.peerManager.peers.containsKey(node2.getRemoteNodeAddress().physicalLocation)
				&& node2.peerManager.peers.containsKey(node1.getRemoteNodeAddress().physicalLocation);
	}

	public static TrNode makeTestNode(final int port, final boolean maintenance, final boolean assimilate, final boolean topologyMaintenace, final boolean broadcast, final int minPeers, final int maxPeers) throws Exception {
		final File nodeDir = TrUtils.createTempDirectory();

		final TrConfig nodeConfig = new TrConfig();

		nodeConfig.udp.listenPort = port;
		nodeConfig.localHostName = "127.0.0.1";
		nodeConfig.peers.runMaintainance = maintenance;
		nodeConfig.peers.assimilate = assimilate;
		nodeConfig.peers.topologyMaintenance = topologyMaintenace;
		nodeConfig.peers.runBroadcast = broadcast;
		nodeConfig.peers.minPeers = minPeers;
		nodeConfig.peers.maxPeers = maxPeers;

		final File joinerPubNodeIdsDir = new File(nodeDir, nodeConfig.publicNodeIdsDir);

		joinerPubNodeIdsDir.mkdir();

		return new TrNode(nodeDir, nodeConfig);
	}

	public static void createTestBidirectionalConnection(final TrNode node1, final TrNode node2) {
		node1.peerManager.addNewPeer(node2.getRemoteNodeAddress(), node2.config.capabilities, node2.peerManager.locInfo.getLocation());
		node2.peerManager.addNewPeer(node1.getRemoteNodeAddress(), node1.config.capabilities, node1.peerManager.locInfo.getLocation());
	}

	/*
	 * This allows deserialzing of RSAPublicKey with gson as a no-arg constructor is needed
	 */
	public static class PublicKeyInstanceCreator implements InstanceCreator<RSAPublicKey> {
		public RSAPublicKey createInstance(final Type type) {
			try {
				final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
				keyGen.initialize(2048);
				final KeyPair key = keyGen.generateKeyPair();
				return (RSAPublicKey) key.getPublic();
			} catch (final NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
