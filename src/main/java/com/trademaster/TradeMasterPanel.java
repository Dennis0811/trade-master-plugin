package com.trademaster;

import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;

public class TradeMasterPanel extends PluginPanel {
    public TradeMasterPanel() {
        final int Y_GAP = 5;
        final int X_GAP = Y_GAP * 2;
        final int MARGIN = 15;

        this.setBorder(BorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));

        ImageIcon scaledHomeIcon = getScaledImageIcon("/home_icon_white_filled.png", 20, 20);
        JButton homeButton = new JButton(scaledHomeIcon);
        homeButton.setToolTipText("Home");
        homeButton.setMargin(new Insets(Y_GAP, X_GAP, Y_GAP, X_GAP));

        ImageIcon scaledGpIcon = getScaledImageIcon("/gp_icon.png", 20, 20);
        JLabel gpLabel = new JLabel(scaledGpIcon);
        gpLabel.setToolTipText("Gold Pieces (GP)");

        long PLAYER_WEALTH = 135884962L;
        JLabel wealthLabel = new JLabel(abbreviateNumber(PLAYER_WEALTH) + " GP");
        wealthLabel.setToolTipText("Bank: " + formatNumber(5232) + " GP\nInventory: " + formatNumber(1356) + " GP\nTotal: " + formatNumber(PLAYER_WEALTH) + " GP");

        JPanel wealthGroup = new JPanel();
        wealthGroup.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, Y_GAP));
        wealthGroup.add(gpLabel);
        wealthGroup.add(wealthLabel);

        JPanel homeWealthGroup = new JPanel();
        homeWealthGroup.setLayout(new BoxLayout(homeWealthGroup, BoxLayout.X_AXIS));
        homeWealthGroup.add(homeButton);
        homeWealthGroup.add(Box.createHorizontalGlue());
        homeWealthGroup.add(wealthGroup);

        ImageIcon watchlistIcon = getScaledImageIcon("/bookmark_icon_white_filled.png", 20, 20);
        JButton watchlistButton = new JButton("Watchlist", watchlistIcon);
        watchlistButton.setMargin(new Insets(Y_GAP, X_GAP, Y_GAP, X_GAP));
        watchlistButton.setHorizontalAlignment(SwingConstants.LEFT);

        ImageIcon offersIcon = getScaledImageIcon("/ge_icon.png", 20, 20);
        JButton offersButton = new JButton("Offers", offersIcon);
        offersButton.setMargin(new Insets(Y_GAP, X_GAP, Y_GAP, X_GAP));
        offersButton.setHorizontalAlignment(SwingConstants.LEFT);

        ImageIcon tradesIcon = getScaledImageIcon("/ledger_icon.png", 20, 20);
        JButton tradesButton = new JButton("Trade History", tradesIcon);
        tradesButton.setMargin(new Insets(Y_GAP, X_GAP, Y_GAP, X_GAP));
        tradesButton.setHorizontalAlignment(SwingConstants.LEFT);

        add(homeWealthGroup);
        add(watchlistButton);
        add(offersButton);
        add(tradesButton);
    }

    private static String formatNumber(long value) {
        return NumberFormat.getNumberInstance().format(value);
    }

    private static String abbreviateNumber(long value) {
//        only abbreviate numbers over 1 billion (1b = 10^9)
        if (value < Math.pow(10, 9)) return formatNumber(value);

        String[] suffixes = {"", "K", "M", "B", "T", "Qa", "Qi", "Oc"};
        int suffixIndex = 0;
        double dividedValue = value;

        while (dividedValue >= 1000 && suffixIndex < suffixes.length - 1) {
            dividedValue /= 1000;
            suffixIndex++;
        }

        return String.format("%.1f%s", dividedValue, suffixes[suffixIndex]);
    }

    private static ImageIcon getScaledImageIcon(String path, int w, int h) {
        ImageIcon homeIcon = new ImageIcon(TradeMasterPlugin.class.getResource(path));
        Image scaledImage = homeIcon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }


}
