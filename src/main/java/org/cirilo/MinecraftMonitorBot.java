package org.cirilo;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MinecraftMonitorBot extends ListenerAdapter {

    private static final String TOKEN = Dotenv.load().get("DISCORD_TOKEN"); // Insira o token do seu bot
    private static final String START_SCRIPT = "tmux new-session -d -s minecraft 'bash /home/ubuntu/mod/start.sh'\n"; // Caminho completo para o script de inicialização
    private static final String MINECRAFT_SERVER_IP = "127.0.0.1"; // IP do servidor (localhost no VPS)
    private static final int MINECRAFT_SERVER_PORT = 25565; // Porta do servidor de Minecraft
    private static final long CHECK_INTERVAL_MINUTES = 5; // Intervalo de verificação (em minutos)

    public static void main(String[] args) throws LoginException {
        JDA jda = JDABuilder.createDefault(TOKEN).enableIntents(EnumSet.of(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)).addEventListeners(new MinecraftMonitorBot()).build();
        System.out.println("Bot iniciado!");

        // Configura o monitoramento automático do servidor
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            if (!isMinecraftServerRunning()) {
                System.out.println("Servidor de Minecraft offline. Tentando iniciar...");
                startMinecraftServer(null); // Passa null, pois não estamos respondendo a uma mensagem
            }
        }, 0, CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignora mensagens de outros bots
        if (event.getAuthor().isBot()) {
            return;
        }

        String message = event.getMessage().getContentRaw();

        // Comando !status para checar o status do servidor
        if (message.equalsIgnoreCase("!status")) {
            if (isMinecraftServerRunning()) {
                event.getChannel().sendMessage("O servidor de Minecraft está online! ✅").queue();
            } else {
                event.getChannel().sendMessage("O servidor de Minecraft está offline! Tentando iniciar...").queue();
                startMinecraftServer(event);
            }
        }

        // Comando !iniciar para iniciar manualmente o servidor
        else if (message.equalsIgnoreCase("!iniciar")) {
            if (isMinecraftServerRunning()) {
                event.getChannel().sendMessage("O servidor de Minecraft já está online! ✅").queue();
            } else {
                event.getChannel().sendMessage("Tentando iniciar o servidor de Minecraft...").queue();
                startMinecraftServer(event);
            }
        }
    }


    // Método para verificar se o servidor está ativo
    private static boolean isMinecraftServerRunning() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(MINECRAFT_SERVER_IP, MINECRAFT_SERVER_PORT), 2000); // Timeout de 2 segundos
            return true; // Conseguiu se conectar, servidor está ativo
        } catch (IOException e) {
            return false; // Não conseguiu se conectar, servidor está offline
        }
    }

    // Método para iniciar o servidor de Minecraft
    private static void startMinecraftServer(MessageReceivedEvent event) {
        try {
            Process process = new ProcessBuilder(START_SCRIPT).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            if (event != null) {
                event.getChannel().sendMessage("Servidor de Minecraft iniciado com sucesso! ✅").queue();
            } else {
                System.out.println("Servidor de Minecraft iniciado com sucesso! ✅");
            }
        } catch (Exception e) {
            if (event != null) {
                event.getChannel().sendMessage("Erro ao iniciar o servidor de Minecraft: " + e.getMessage()).queue();
            } else {
                System.out.println("Erro ao iniciar o servidor de Minecraft: " + e.getMessage());
            }
        }
    }
}
