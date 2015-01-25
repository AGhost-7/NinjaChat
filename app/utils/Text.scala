package utils

object Text {
		// Need to escape the text since this isn't going through the default 
		// template escaping. User names are already sanitized. Instead of going 
		// through the string several times, traverse it once.
		// Reference:
		// https://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet
		def escape(msg: String) = {
			val r = new StringBuilder
			val i = msg.iterator
			while(i.hasNext){
				i.next match {
					case '&' => r.append("&amp;")
					case '<' => r.append("&lt;")
					case '>' => r.append("&gt;")
					case '"' => r.append("&quot;")
					case ''' => r.append("&#x27;")
					case '/' => r.append("&#x2F;")
					case c => r.append(c)
				}
			}
			r.toString
		}
}