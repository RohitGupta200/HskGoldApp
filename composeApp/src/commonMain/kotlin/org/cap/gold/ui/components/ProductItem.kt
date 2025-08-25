package org.cap.gold.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import org.cap.gold.data.model.Product
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun ProductItem(
    product: Product,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onProductClick(product.id) },
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier.padding(4.dp),

        ) {
            // Product Image
            @OptIn(ExperimentalEncodingApi::class)
            val model: Any? = when {
                !product.imageBase64.isNullOrEmpty() -> runCatching { Base64.decode(product.imageBase64) }.getOrNull()
                product.imageUrl.isNotEmpty() -> product.imageUrl
                else -> null
            }
            val painter = rememberAsyncImagePainter(model = model)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            ) {
                if (model != null) {
                    Image(
                        painter = painter,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Product Name
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Product Price
            Text(
                text = "₹${product.price}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
