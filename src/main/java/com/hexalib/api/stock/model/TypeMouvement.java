package com.hexalib.api.stock.model;

public enum TypeMouvement {
    ENTREE,      // Réapprovisionnement, réception commande
    SORTIE,      // Vente (automatique)
    AJUSTEMENT,  // Inventaire, correction, perte, don
    RETOUR       // Annulation de vente
}