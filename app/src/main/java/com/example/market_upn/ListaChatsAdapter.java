package com.example.market_upn;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ListaChatsAdapter extends RecyclerView.Adapter<ListaChatsAdapter.ChatViewHolder> {

    private Context context;
    private List<ChatResumen> listaChats;
    private SimpleDateFormat sdf;

    public ListaChatsAdapter(Context context, List<ChatResumen> listaChats) {
        this.context = context;
        this.listaChats = listaChats;
        this.sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lista_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatResumen chat = listaChats.get(position);

        holder.tvNombreUsuario.setText(chat.getOtroUsuarioNombre() != null ? chat.getOtroUsuarioNombre() : "Cargando...");
        holder.tvProducto.setText("Producto: " + (chat.getTituloProducto() != null ? chat.getTituloProducto() : ""));
        holder.tvUltimoMensaje.setText(chat.getUltimoMensaje() != null ? chat.getUltimoMensaje() : "");
        
        if (chat.getFechaUltimoMensaje() != null) {
            holder.tvFecha.setText(sdf.format(chat.getFechaUltimoMensaje()));
        } else {
            holder.tvFecha.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("receptorId", chat.getOtroUsuarioId());
            intent.putExtra("productoId", chat.getProductoId());
            intent.putExtra("nombreVendedor", chat.getOtroUsuarioNombre());
            intent.putExtra("tituloProducto", chat.getTituloProducto());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listaChats.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreUsuario, tvProducto, tvUltimoMensaje, tvFecha;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreUsuario = itemView.findViewById(R.id.tvChatNombreUsuario);
            tvProducto = itemView.findViewById(R.id.tvChatProducto);
            tvUltimoMensaje = itemView.findViewById(R.id.tvChatUltimoMensaje);
            tvFecha = itemView.findViewById(R.id.tvChatFecha);
        }
    }
}
