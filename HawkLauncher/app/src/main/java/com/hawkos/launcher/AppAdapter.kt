override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val tv = (convertView as? TextView) ?: TextView(context)
    val app = apps[position]
    
    tv.text = "> ${app.label.uppercase()}" // Niagara-style text list
    tv.setTextColor(Color.parseColor("#00FF9F"))
    tv.typeface = Typeface.MONOSPACE
    tv.textSize = 14f
    tv.setPadding(60, 25, 20, 25) // Extra left padding for the side-swipe feel
    
    return tv
}
