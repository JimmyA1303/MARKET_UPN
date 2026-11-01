package com.example.market_upn;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificacionAdapter extends RecyclerView.Adapter<NotificacionAdapter.ViewHolder> {

    private List<Notificacion> notificacionesList;
    private Context context;
    private OnNotificacionClickListener listener;

    public interface OnNotificacionClickListener {
        void onNotificacionClick(Notificacion notificacion);
    }

    public NotificacionAdapter(List<Notificacion> notificacionesList, Context context, OnNotificacionClickListener listener) {
        this.notificacionesList = notificacionesList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notificacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notificacion noti = notificacionesList.get(position);

        holder.tvTitulo.setText(noti.getTitulo());
        holder.tvMensaje.setText(noti.getMensaje());

        if (noti.getFechaCreacion() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
            holder.tvFecha.setText(sdf.format(noti.getFechaCreacion()));
        } else {
            holder.tvFecha.setText("Ahora");
        }

        if (noti.isLeida()) {
            holder.puntoNoLeido.setVisibility(View.GONE);
            holder.cardNotificacion.setCardBackgroundColor(Color.parseColor("#F7F9FC"));
        } else {
            holder.puntoNoLeido.setVisibility(View.VISIBLE);
            holder.cardNotificacion.setCardBackgroundColor(Color.WHITE);
        }

        if ("MENSAJE".equals(noti.getTipo())) {
            holder.cardIconoFondo.setCardBackgroundColor(Color.parseColor("#E0E7FF"));
            holder.ivIcono.setColorFilter(Color.parseColor("#4F46E5"));

        } else if ("PUBLICACION".equals(noti.getTipo())) {
            holder.cardIconoFondo.setCardBackgroundColor(Color.parseColor("#FFF2D4"));
            holder.ivIcono.setColorFilter(Color.parseColor("#D99B00")); // Mostaza
        }

        holder.itemView.setOnClickListener(v -> listener.onNotificacionClick(noti));
    }

    @Override
    public int getItemCount() {
        return notificacionesList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvMensaje, tvFecha;
        MaterialCardView cardNotificacion, cardIconoFondo;
        ImageView ivIcono;
        View puntoNoLeido;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTituloNotificacion);
            tvMensaje = itemView.findViewById(R.id.tvMensajeNotificacion);
            tvFecha = itemView.findViewById(R.id.tvFechaNotificacion);
            cardNotificacion = itemView.findViewById(R.id.cardNotificacion);
            cardIconoFondo = itemView.findViewById(R.id.cardIconoFondo);
            ivIcono = itemView.findViewById(R.id.ivIconoNotificacion);
            puntoNoLeido = itemView.findViewById(R.id.puntoNoLeido);
        }
    }
}