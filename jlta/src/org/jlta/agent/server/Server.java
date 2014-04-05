package org.jlta.agent.server;

import org.jlta.agent.Tracking;
import org.jlta.common.TrackingData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;

public class Server extends Thread
{
  private final ServerSocket serverSocket;

  public Server(int port) throws IOException
  {
    super("JLTA Server");
    serverSocket = new ServerSocket(port);
    System.out.println("## (JLTA) ## Listening on port: " + serverSocket.getLocalPort());

    setDaemon(true);
    start();
  }

  @Override
  public void run()
  {
    try
    {
      while (true)
      {
        Socket clientSocket = serverSocket.accept();
        new Client(clientSocket);
      }
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
    System.out.println("## (JLTA) ## Server offline on port: " + serverSocket.getLocalPort());
  }

  private static class Client extends Thread
  {
    private final Socket clientSocket;

    public Client(Socket clientSocket) throws SocketException
    {
      super("JLTA Client - " + clientSocket.getRemoteSocketAddress());
      this.clientSocket = clientSocket;
      System.out.println("## (JLTA) ## Client connected: " + clientSocket.getRemoteSocketAddress());
      this.clientSocket.setKeepAlive(true);

      setDaemon(true);
      start();
    }

    @Override
    public void run()
    {
      try
      {
        ObjectInputStream objIn = new ObjectInputStream(clientSocket.getInputStream());
        ObjectOutputStream objOut = new ObjectOutputStream(clientSocket.getOutputStream());

        while(true)
        {
          String message = (String)objIn.readObject();
          if ("reset".equals(message))
          {
            Tracking.data.clear();
            System.out.println("## (JLTA) ## Data Cleared - " +
                               new Date().toString());
          }
          else if ("fetch".equals(message))
          {
            TrackingData copy = Tracking.data.staticCopy();
            objOut.writeObject(copy);
            objOut.flush();
            
            // Reset the output stream to avoid caching any of the ThreadData objects
            // which we just sent.
            objOut.reset();
          }
          else if ("prune".equals(message))
          {
              Tracking.data.prune();
          }
          else
          {
            System.out.println("## (JLTA) ## Unrecognised command: " + message +
                               " - " + new Date().toString());
          }
        }
      }
      catch (IOException ex)
      {
        System.out.println("## (JLTA) ## Disconnected: " +
                           clientSocket.getRemoteSocketAddress() +
                           " - " + new Date().toString());
      }
      catch (ClassNotFoundException ex)
      {
        System.out.println("## (JLTA) ## Error: " +
                           ex.toString() + " - " +
                           new Date().toString());
      }
    }
  }
}
