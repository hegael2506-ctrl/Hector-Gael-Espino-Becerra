package reproductor;

import javazoom.jl.player.Player;
import javazoom.jl.decoder.JavaLayerException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import javax.sound.sampled.*;

/**
 *
 * @author Hector
 */
public class ReproductorMP3Interactivo extends JFrame {

    private JButton btnAnterior, btnPlay, btnPausa, btnStop, btnSiguiente;
    private JLabel lblCancion, lblCaratula, lblTiempo;
    private JSlider barraProgreso, barraVolumen;
    private Timer timerProgreso;
    private ArrayList<String> listaCanciones;
    private ArrayList<String> listaCaratulas;
    private int indiceActual = 0;

    private Player player;
    private Thread hiloReproduccion;
    private FileInputStream fis;
    private long totalLength;
    private long pausaLocation;
    private boolean enPausa = false;
    private boolean usuarioArrastrando = false;
    private int segundosTranscurridos = 0;
    private int duracionEstimada = 0;

    public ReproductorMP3Interactivo() {
        super("Reproductor MP3");

        //Lista de canciones y fondo
        
        listaCanciones = new ArrayList<>();
        listaCanciones.add("C:\\Users\\Hector Gael\\Documents\\NetBeansProjects\\ReproductorMP3_2\\src\\music\\Él (Him).mp3");
        listaCanciones.add("C:\\Users\\Hector Gael\\Documents\\NetBeansProjects\\ReproductorMP3_2\\src\\music\\Electric Guest - Awake.mp3");
        listaCanciones.add("C:\\Users\\Hector Gael\\Documents\\NetBeansProjects\\ReproductorMP3_2\\src\\music\\Little Dark Age.mp3");
        listaCanciones.add("C:\\Users\\Hector Gael\\Documents\\NetBeansProjects\\ReproductorMP3_2\\src\\music\\Do I Wanna Know.mp3");
        listaCanciones.add("C:\\Users\\Hector Gael\\Documents\\NetBeansProjects\\ReproductorMP3_2\\src\\music\\Borderline.mp3");
        
        listaCaratulas = new ArrayList<>();
        listaCaratulas.add("C:\\Users\\Hector Gael\\Documents\\NetBeansProjects\\ReproductorMP3_2\\src\\images\\El.jpeg");
        listaCaratulas.add("C:\\Users\\Hector Gael\\Documents\\NetBeansProjects\\ReproductorMP3_2\\src\\images\\electric.jpeg");
        listaCaratulas.add("C:\\Users\\Hector Gael\\Documents\\NetBeansProjects\\ReproductorMP3_2\\src\\images\\Little.jpeg");
        listaCaratulas.add("C:\\Users\\Hector Gael\\Documents\\NetBeansProjects\\ReproductorMP3_2\\src\\images\\Do.jpeg");
        listaCaratulas.add("C:\\Users\\Hector Gael\\Documents\\NetBeansProjects\\ReproductorMP3_2\\src\\images\\Borderline.jpeg");

        // --- Componentes ---
        lblCancion = new JLabel("Selecciona una cancion.", SwingConstants.CENTER);
        lblCancion.setFont(new Font("Arial", Font.BOLD, 16));

        lblCaratula = new JLabel();
        lblCaratula.setHorizontalAlignment(SwingConstants.CENTER);
        lblCaratula.setPreferredSize(new Dimension(200, 200));
        actualizarCaratula();

        lblTiempo = new JLabel("00:00 / 00:00", SwingConstants.CENTER);
        lblTiempo.setFont(new Font("Consolas", Font.PLAIN, 14));

        barraProgreso = new JSlider(0, 100, 0);
        barraVolumen = new JSlider(0, 100, 80);
        barraVolumen.setToolTipText("Volumen");
        barraVolumen.addChangeListener(e -> ajustarVolumen(barraVolumen.getValue()));

        btnAnterior = new JButton("Cancion Anterior");
        btnPlay = new JButton("Play");
        btnPausa = new JButton("Pausa");
        btnStop = new JButton("Stop");
        btnSiguiente = new JButton("Siguiente Cancion");

        JPanel panelBotones = new JPanel();
        panelBotones.add(btnAnterior);
        panelBotones.add(btnPlay);
        panelBotones.add(btnPausa);
        panelBotones.add(btnStop);
        panelBotones.add(btnSiguiente);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(lblCaratula, BorderLayout.CENTER);
        panelCentro.add(lblCancion, BorderLayout.SOUTH);

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.add(lblTiempo, BorderLayout.NORTH);
        panelInferior.add(barraProgreso, BorderLayout.CENTER);
        //panelInferior.add(barraVolumen, BorderLayout.SOUTH);

        add(panelCentro, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.NORTH);
        add(panelInferior, BorderLayout.SOUTH);

        setSize(500, 420);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- Eventos ---
        btnPlay.addActionListener(e -> reproducir());
        btnPausa.addActionListener(e -> pausar());
        btnStop.addActionListener(e -> detener());
        btnSiguiente.addActionListener(e -> siguiente());
        btnAnterior.addActionListener(e -> anterior());

        // --- Interactividad de la barra de progreso ---
        barraProgreso.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                usuarioArrastrando = true;
            }

            public void mouseReleased(MouseEvent e) {
                usuarioArrastrando = false;
                int valor = barraProgreso.getValue();
                saltarA(valor);
            }
        });

        barraProgreso.addChangeListener(e -> {
            if (usuarioArrastrando) {
                int valor = barraProgreso.getValue();
                int nuevoTiempo = (int) ((valor / 100.0) * duracionEstimada);
                lblTiempo.setText(formatoTiempo(nuevoTiempo) + " / " + formatoTiempo(duracionEstimada));
            }
        });

        // --- Temporizador ---
        timerProgreso = new Timer(1000, e -> actualizarBarraProgreso());
    }

    private void reproducir() {
        try {
            if (enPausa) {
                fis = new FileInputStream(listaCanciones.get(indiceActual));
                fis.skip(totalLength - pausaLocation);
                player = new Player(fis);
                enPausa = false;
            } else {
                detener();
                fis = new FileInputStream(listaCanciones.get(indiceActual));
                totalLength = fis.available();
                player = new Player(fis);
                lblCancion.setText("Reproduciendo: " + new File(listaCanciones.get(indiceActual)).getName());
                actualizarCaratula();
                segundosTranscurridos = 0;
                duracionEstimada = (int) (totalLength / 32000);
                lblTiempo.setText("00:00 / " + formatoTiempo(duracionEstimada));
            }

            hiloReproduccion = new Thread(() -> {
                try {
                    timerProgreso.start();
                    player.play();
                    timerProgreso.stop();
                } catch (JavaLayerException ex) {
                    System.out.println("Error al reproducir: " + ex.getMessage());
                }
            });
            hiloReproduccion.start();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al reproducir: " + ex.getMessage());
        }
    }

    private void pausar() {
        try {
            if (player != null) {
                pausaLocation = fis.available();
                player.close();
                enPausa = true;
                lblCancion.setText("Pausado: " + new File(listaCanciones.get(indiceActual)).getName());
                timerProgreso.stop();
            }
        } catch (Exception ex) {
            System.out.println("Error al pausar: " + ex.getMessage());
        }
    }

    private void detener() {
        try {
            if (player != null) {
                player.close();
                hiloReproduccion = null;
                lblCancion.setText("Detenido");
                barraProgreso.setValue(0);
                lblTiempo.setText("00:00 / 00:00");
                timerProgreso.stop();
            }
        } catch (Exception ex) {
            System.out.println("Error al detener: " + ex.getMessage());
        }
    }

    private void siguiente() {
        detener();
        indiceActual = (indiceActual + 1) % listaCanciones.size();
        reproducir();
    }

    private void anterior() {
        detener();
        indiceActual = (indiceActual - 1 + listaCanciones.size()) % listaCanciones.size();
        reproducir();
    }

    private void actualizarCaratula() {
        try {
            ImageIcon icon = new ImageIcon(listaCaratulas.get(indiceActual));
            Image img = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
            lblCaratula.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            lblCaratula.setIcon(null);
        }
    }

    private void actualizarBarraProgreso() {
        try {
            if (fis != null && totalLength > 0 && !usuarioArrastrando) {
                long restante = fis.available();
                int progreso = (int) (100 - (restante * 100 / totalLength));
                barraProgreso.setValue(progreso);
                segundosTranscurridos++;
                lblTiempo.setText(formatoTiempo(segundosTranscurridos) + " / " + formatoTiempo(duracionEstimada));
            }
        } catch (IOException e) {
            barraProgreso.setValue(0);
        }
    }

    private void saltarA(int porcentaje) {
        try {
            if (player != null) {
                player.close();
                long nuevaPos = (totalLength * porcentaje) / 100;
                fis = new FileInputStream(listaCanciones.get(indiceActual));
                fis.skip(nuevaPos);
                pausaLocation = fis.available();
                player = new Player(fis);
                segundosTranscurridos = (int) ((porcentaje / 100.0) * duracionEstimada);

                hiloReproduccion = new Thread(() -> {
                    try {
                        timerProgreso.start();
                        player.play();
                        timerProgreso.stop();
                    } catch (JavaLayerException ex) {
                        System.out.println("Error al saltar: " + ex.getMessage());
                    }
                });
                hiloReproduccion.start();
            }
        } catch (Exception ex) {
            System.out.println("Error al saltar: " + ex.getMessage());
        }
    }

    private void ajustarVolumen(int valor) {
        try {
            float volumen = valor / 100f;
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            for (Mixer.Info mixerInfo : mixers) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                Line.Info[] lineInfos = mixer.getTargetLineInfo();
                for (Line.Info lineInfo : lineInfos) {
                    Line line = mixer.getLine(lineInfo);
                    if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                        float dB = (float) (Math.log(volumen == 0.0 ? 0.0001 : volumen) / Math.log(10.0) * 20.0);
                        control.setValue(dB);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String formatoTiempo(int segundos) {
        int min = segundos / 60;
        int seg = segundos % 60;
        return String.format("%02d:%02d", min, seg);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        SwingUtilities.invokeLater(() -> new ReproductorMP3Interactivo().setVisible(true));
    }
}
