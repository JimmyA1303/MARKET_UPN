package com.example.market_upn;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MensajeAdapter extends RecyclerView.Adapter<MensajeAdapter.MensajeViewHolder> {

    private List<Mensaje> listaMensajes;
    private String miId;

    public MensajeAdapter(List<Mensaje> listaMensajes, String miId) {
        this.listaMensajes = listaMensajes;
        this.miId = miId;
    }

    @NonNull
    @Override
    public MensajeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mensaje_chat, parent, false);
        return new MensajeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MensajeViewHolder holder, int position) {
        Mensaje mensaje = listaMensajes.get(position);
        holder.tvTextoMensaje.setText(mensaje.getTexto());

        boolean esMio = mensaje.getRemitenteId() != null && mensaje.getRemitenteId().equals(miId);

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dpToPx(holder.itemView, 18));

        if (esMio) {
            holder.espacioIzquierda.setVisibility(View.VISIBLE);
            holder.espacioDerecha.setVisibility(View.GONE);
            
            background.setColor(Color.parseColor("#102A43")); // Azul oscuro (Mío)
            holder.tvTextoMensaje.setTextColor(Color.WHITE);
        } else {
            holder.espacioIzquierda.setVisibility(View.GONE);
            holder.espacioDerecha.setVisibility(View.VISIBLE);
            
            background.setColor(Color.parseColor("#E2E8F0")); // Gris claro (Otro)
            holder.tvTextoMensaje.setTextColor(Color.parseColor("#102A43"));
        }
        
        holder.tvTextoMensaje.setBackground(background);
    }

    @Override
    public int getItemCount() {
        return listaMensajes.size();
    }

    private int dpToPx(View view, int dp) {
        return Math.round(dp * view.getResources().getDisplayMetrics().density);
    }

    public static class MensajeViewHolder extends RecyclerView.ViewHolder {
        TextView tvTextoMensaje;
        Space espacioIzquierda, espacioDerecha;

        public MensajeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTextoMensaje = itemView.findViewById(R.id.tvTextoMensaje);
            espacioIzquierda = itemView.findViewById(R.id.espacioIzquierda);
            espacioDerecha = itemView.findViewById(R.id.espacioDerecha);
        }
    }
}