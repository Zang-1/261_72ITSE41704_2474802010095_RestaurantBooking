"""
Builds the Final Project Report (DOCX) from the members' Markdown parts and the course template.

    python docs/report/build_report.py en     -> docs/report/output/2474802010095_KieuBaoGiang_ApplicationProgrammingProject.docx
    python docs/report/build_report.py vi     -> docs/report/output/Bản tiếng Việt.docx  (uses the *.vi.md files)

Run from the repository root. The cover page, the declaration form and the page setup come from
Template_Final-Project-Report.docx; the chapter outline of the template is replaced by:
Chapter 1 (giang), Chapter 2 (lam), Chapter 3 (the three module parts merged section by section),
Chapter 4 (giang, with Lam's and Khoa's rows), Appendices A-D and the references.
Images named in backticks (`...png`, `...jpg`) are embedded as numbered figures.
Export to PDF and refresh the tables of contents with export_pdf.ps1 (needs Microsoft Word).
"""
import copy
import re
import sys
from pathlib import Path

import docx
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Emu, Inches, Pt, RGBColor

LANG = sys.argv[1] if len(sys.argv) > 1 else 'en'
SUBMIT_DATE = '27/11/2026'           # cover and declaration (Guidelines §8): change to the day of submission
ROOT = Path.cwd()
REPORT = ROOT / 'docs' / 'report'
TEMPLATE = ROOT.parent / 'Template_Final-Project-Report.docx'
OUT = REPORT / 'output' / {'en': '2474802010095_KieuBaoGiang_ApplicationProgrammingProject.docx', 'vi': 'Bản tiếng Việt.docx'}[LANG]
FONT = 'Times New Roman'
SUFFIX = '.vi.md' if LANG == 'vi' else '.md'

T = {
    'en': {
        'title': 'RESTAURANT TABLE BOOKING AND KITCHEN ORDER SYSTEM',
        'presented': 'Presented by:',
        'members': ['Kieu Bao Giang – 2474802010095 (Leader)', 'Tang Thoai Lam – 2474802010210 (Member)', 'Nguyen Nam Khoa – 2474802010194 (Member)'],
        'names': 'Kieu Bao Giang, Tang Thoai Lam, Nguyen Nam Khoa',
        'names_ids': 'Kieu Bao Giang (2474802010095), Tang Thoai Lam (2474802010210) and Nguyen Nam Khoa (2474802010194)',
        'course': '72ITSE41704 · Class group 261_72ITSE41704_01',
        'city': 'Ho Chi Minh City',
        'signers': 'Kieu Bao Giang          Tang Thoai Lam          Nguyen Nam Khoa',
        'ch': ['CHAPTER 1: INTRODUCTION', 'CHAPTER 2: THEORETICAL BACKGROUND AND CURRENT SITUATION',
               'CHAPTER 3: SYSTEM ANALYSIS, DESIGN, AND IMPLEMENTATION', 'CHAPTER 4: CONCLUSION'],
        'ch3': [('3.1', '3.1 Problem Description and System Objectives', 2), ('3.2', '3.2 System Analysis and Design', 2),
                ('3.2.1', '3.2.1 Functional Analysis', 3), ('3.2.2', '3.2.2 Data Flow Analysis', 3),
                ('3.2.3', '3.2.3 Database Design', 3), ('3.2.4', '3.2.4 Class Design and State Machines', 3),
                ('3.2.5', '3.2.5 Interface Design', 3), ('3.3', '3.3 System Development and Implementation', 2),
                ('3.3.1', '3.3.1 Technologies and Tools Used', 3), ('3.3.2', '3.3.2 Implemented Functions', 3),
                ('3.3.3', '3.3.3 Unimplemented or Incomplete Functions', 3), ('3.3.4', '3.3.4 Other Deliverables', 3),
                ('3.4', '3.4 Testing and Evaluation', 2), ('3.4.1', '3.4.1 Testing Process', 3),
                ('3.4.2', '3.4.2 System Evaluation', 3), ('3.4.3', '3.4.3 Comparison Between Theory and Practice', 3)],
        'modules': {'giang': 'Table & Reservation module (Kieu Bao Giang)', 'lam': 'Order & Kitchen module (Tang Thoai Lam)',
                    'khoa': 'Billing & Reporting module (Nguyen Nam Khoa)'},
        'appendices': 'APPENDICES', 'references': 'REFERENCES',
        'appA': 'Appendix A – Use case specifications', 'appB': 'Appendix B – Declaration of AI-tool use',
        'appC': 'Appendix C – Individual contribution table', 'appD': 'Appendix D – Full test-case log',
        'appA_note': 'The use cases as specified in the Project Proposal. Requirement changes after the restaurant interview (CR-01 to CR-05) are listed in §2.3.6 and apply to these use cases.',
        'appC_note': 'Commits: commits on the main branch on 27/11/2026 by each member\'s accounts, merge commits excluded, as counted by GitHub Contributors. Share of work follows the commit counts.',
        'declare': 'We declare that we understand every part of the submitted work and can explain it on request.',
        'testlogs': [('tests/test-cases-table-reservation.md', 'D.1 Table & Reservation (TC-RES)'),
                     ('tests/test-cases-order-kitchen.md', 'D.2 Order & Kitchen (TC-MENU, TC-ORD, TC-KIT)'),
                     ('tests/test-cases-billing.md', 'D.3 Billing & Reporting (TC-BIL, TC-SHF)')],
        'figure': 'Figure', 'table': 'Table',
        'decl_name': '(Your Full Name)',
    },
    'vi': {
        'title': 'HỆ THỐNG ĐẶT BÀN VÀ GỌI MÓN CHO NHÀ HÀNG',
        'presented': 'Sinh viên thực hiện:',
        'members': ['Kiều Bảo Giang – 2474802010095 (Nhóm trưởng)', 'Tăng Thoại Lâm – 2474802010210 (Thành viên)', 'Nguyễn Nam Khoa – 2474802010194 (Thành viên)'],
        'names': 'Kiều Bảo Giang, Tăng Thoại Lâm, Nguyễn Nam Khoa',
        'names_ids': 'Kiều Bảo Giang (2474802010095), Tăng Thoại Lâm (2474802010210) và Nguyễn Nam Khoa (2474802010194)',
        'course': '72ITSE41704 · Nhóm lớp 261_72ITSE41704_01',
        'city': 'TP. Hồ Chí Minh',
        'signers': 'Kiều Bảo Giang          Tăng Thoại Lâm          Nguyễn Nam Khoa',
        'ch': ['CHƯƠNG 1: GIỚI THIỆU', 'CHƯƠNG 2: CƠ SỞ LÝ THUYẾT VÀ HIỆN TRẠNG',
               'CHƯƠNG 3: PHÂN TÍCH, THIẾT KẾ VÀ HIỆN THỰC HỆ THỐNG', 'CHƯƠNG 4: KẾT LUẬN'],
        'ch3': [('3.1', '3.1 Mô tả bài toán và mục tiêu hệ thống', 2), ('3.2', '3.2 Phân tích và thiết kế hệ thống', 2),
                ('3.2.1', '3.2.1 Phân tích chức năng', 3), ('3.2.2', '3.2.2 Phân tích luồng dữ liệu', 3),
                ('3.2.3', '3.2.3 Thiết kế cơ sở dữ liệu', 3), ('3.2.4', '3.2.4 Thiết kế lớp và máy trạng thái', 3),
                ('3.2.5', '3.2.5 Thiết kế giao diện', 3), ('3.3', '3.3 Phát triển và hiện thực hệ thống', 2),
                ('3.3.1', '3.3.1 Công nghệ và công cụ', 3), ('3.3.2', '3.3.2 Các chức năng đã hiện thực', 3),
                ('3.3.3', '3.3.3 Chức năng chưa hiện thực hoặc chưa hoàn chỉnh', 3), ('3.3.4', '3.3.4 Sản phẩm khác', 3),
                ('3.4', '3.4 Kiểm thử và đánh giá', 2), ('3.4.1', '3.4.1 Quy trình kiểm thử', 3),
                ('3.4.2', '3.4.2 Đánh giá hệ thống', 3), ('3.4.3', '3.4.3 So sánh lý thuyết và thực tế', 3)],
        'modules': {'giang': 'Phân hệ Bàn & Đặt bàn (Kiều Bảo Giang)', 'lam': 'Phân hệ Gọi món & Bếp (Tăng Thoại Lâm)',
                    'khoa': 'Phân hệ Thanh toán & Báo cáo (Nguyễn Nam Khoa)'},
        'appendices': 'PHỤ LỤC', 'references': 'TÀI LIỆU THAM KHẢO',
        'appA': 'Phụ lục A – Đặc tả use case', 'appB': 'Phụ lục B – Khai báo sử dụng công cụ AI',
        'appC': 'Phụ lục C – Bảng đóng góp của từng thành viên', 'appD': 'Phụ lục D – Nhật ký test case đầy đủ',
        'appA_note': 'Các use case như trong Project Proposal. Những thay đổi yêu cầu sau buổi phỏng vấn nhà hàng (CR-01 đến CR-05) được liệt kê ở mục 2.3.6 và áp dụng cho các use case này.',
        'appC_note': 'Số commit: commit trên nhánh main ngày 27/11/2026 của các tài khoản của từng thành viên, không tính commit merge, giống cách GitHub Contributors đếm. Tỉ lệ đóng góp tính theo số commit.',
        'declare': 'Chúng tôi cam kết hiểu mọi phần của sản phẩm đã nộp và có thể giải thích khi được yêu cầu.',
        'testlogs': [('tests/test-cases-table-reservation.md', 'D.1 Bàn & Đặt bàn (TC-RES)'),
                     ('tests/test-cases-order-kitchen.md', 'D.2 Gọi món & Bếp (TC-MENU, TC-ORD, TC-KIT)'),
                     ('tests/test-cases-billing.md', 'D.3 Thanh toán & Báo cáo (TC-BIL, TC-SHF)')],
        'figure': 'Hình', 'table': 'Bảng',
        'appD_note': 'Nhật ký test case được giữ nguyên bằng tiếng Anh như trong kho mã (`tests/`), vì mã test case, tên kiểm thử tự động và thông báo lỗi của hệ thống đều bằng tiếng Anh.',
        'decl_name': '(Your Full Name)',
    },
}[LANG]

# ---------------------------------------------------------------- Markdown helpers

def read(path):
    p = ROOT / path
    vi = p.with_name(p.name.replace('.md', '.vi.md'))
    if LANG == 'vi' and vi.exists():
        p = vi
    return p.read_text(encoding='utf-8').replace('\r\n', '\n')


def section(text, title_re):
    """Lines under the first heading matching title_re, up to the next heading of the same or a higher level."""
    lines = text.split('\n')
    for i, line in enumerate(lines):
        m = re.match(r'^(#+)\s+(.*)$', line)
        if m and re.search(title_re, m.group(2)):
            level = len(m.group(1))
            out = []
            for nxt in lines[i + 1:]:
                n = re.match(r'^(#+)\s', nxt)
                if n and len(n.group(1)) <= level:
                    break
                out.append(nxt)
            return out
    raise SystemExit(f'section not found: {title_re}')


def numbered_parts(lines):
    """{'3.2.1': [lines], ...} from the ###/#### headings that start with a section number."""
    parts, key = {}, None
    for line in lines:
        m = re.match(r'^#{3,4}\s+(\d+(?:\.\d+)+)\b', line)
        if m:
            key = m.group(1)
            parts[key] = []
        elif key:
            parts[key].append(line)
    return parts


def table_rows(lines):
    """Data rows of the first Markdown table in lines (header and separator removed)."""
    rows = [l for l in lines if l.strip().startswith('|')]
    return rows[2:]


# ---------------------------------------------------------------- document helpers

doc = docx.Document(str(TEMPLATE))
body = doc.element.body
IMAGES = {}           # resolved path -> figure number
counters = {'Figure': 0, 'Table': 0}
image_index = {}
for p in (ROOT / 'docs').rglob('*'):
    if p.suffix.lower() in ('.png', '.jpg', '.jpeg'):
        image_index.setdefault(p.name, []).append(p)


def set_run_font(run, size=13, bold=None, italic=None, mono=False, color=None):
    name = 'Consolas' if mono else FONT
    run.font.name = name
    rpr = run._r.get_or_add_rPr()
    fonts = rpr.find(qn('w:rFonts'))
    if fonts is None:
        fonts = OxmlElement('w:rFonts')
        rpr.insert(0, fonts)
    for attr in ('w:ascii', 'w:hAnsi', 'w:cs', 'w:eastAsia'):
        fonts.set(qn(attr), name)
    run.font.size = Pt(size)
    if bold is not None:
        run.bold = bold
    if italic is not None:
        run.italic = italic
    if color:
        run.font.color.rgb = RGBColor.from_string(color)


def spacing(p, before=0, after=6, line=1.5, keep_next=False):
    pf = p.paragraph_format
    pf.space_before = Pt(before)
    pf.space_after = Pt(after)
    pf.line_spacing = line
    if keep_next:
        pf.keep_with_next = True


INLINE = re.compile(r'(\*\*[^*]+\*\*|`[^`]+`|\*[^*\s][^*]*\*|\[[^\]]+\]\([^)]+\))')


def add_inline(p, text, size=13, bold=False, italic=False):
    text = text.replace('\\|', '|')
    for piece in INLINE.split(text):
        if not piece:
            continue
        if piece.startswith('**') and piece.endswith('**'):
            add_inline(p, piece[2:-2], size, True, italic)
        elif piece.startswith('`') and piece.endswith('`'):
            r = p.add_run(piece[1:-1])
            set_run_font(r, size - 1.5, bold=bold, italic=italic, mono=True)
        elif piece.startswith('[') and '](' in piece:
            r = p.add_run(piece[1:piece.index('](')])
            set_run_font(r, size, bold=bold, italic=italic)
        elif piece.startswith('*') and piece.endswith('*') and len(piece) > 2:
            add_inline(p, piece[1:-1], size, bold, True)
        else:
            r = p.add_run(piece)
            set_run_font(r, size, bold=bold, italic=italic)


def new_par():
    return doc.add_paragraph()


def page_break():
    p = new_par()
    p.add_run().add_break(WD_BREAK.PAGE)


def heading(text, level):
    p = doc.add_paragraph(style=f'Heading {level}')
    size = {1: 14, 2: 13, 3: 13, 4: 13}[level]
    r = p.add_run(text)
    set_run_font(r, size, bold=True, italic=(level == 4), color='000000')
    if level == 1:
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    spacing(p, before=12 if level > 1 else 0, after=6, keep_next=True)
    return p


def field(p, instr, placeholder='', first=False):
    """A Word field (TOC, SEQ); Word fills it when the document is exported.
    first=True puts it before the paragraph's own runs: the template ends those paragraphs with a page break."""
    def fld(kind):
        el = OxmlElement('w:fldChar')
        el.set(qn('w:fldCharType'), kind)
        r = OxmlElement('w:r')
        r.append(el)
        return r
    instr_run = OxmlElement('w:r')
    t = OxmlElement('w:instrText')
    t.set(qn('xml:space'), 'preserve')
    t.text = f' {instr} '
    instr_run.append(t)
    shown = OxmlElement('w:r')
    st = OxmlElement('w:t')
    st.text = placeholder
    shown.append(st)
    parts = [fld('begin'), instr_run, fld('separate'), shown, fld('end')]
    at = (1 if p._p.pPr is not None else 0) if first else len(p._p)
    for k, el in enumerate(parts):
        p._p.insert(at + k, el)


def caption(kind, text):
    counters[kind] += 1
    p = doc.add_paragraph()
    try:
        p.style = doc.styles['Caption']
    except KeyError:
        pass
    label = T['figure'] if kind == 'Figure' else T['table']
    r = p.add_run(f'{label} ')
    set_run_font(r, 12, italic=True)
    field(p, f'SEQ {kind} \\* ARABIC', str(counters[kind]))
    for run in p.runs[1:]:
        set_run_font(run, 12, italic=True)
    r = p.add_run(f'. {text}')
    set_run_font(r, 12, italic=True)
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    spacing(p, before=3, after=9, line=1.15)
    return counters[kind]


def resolve_image(token, owner):
    token = token.strip()
    path = ROOT / token
    if '/' in token and path.exists():
        return path
    found = image_index.get(Path(token).name, [])
    if not found:
        return None
    for p in found:
        if owner and owner in p.as_posix():
            return p
    return found[0]


KINDS = {
    'en': {'class': 'Class diagram', 'erd': 'ERD', 'state': 'State diagram', 'sequence': 'Sequence diagram',
           'activity': 'Activity diagram', 'dfd': 'Data flow diagram', 'wireframe': 'Wireframe', 'use': 'Use case diagram',
           'functional': 'Functional decomposition', 'screen': 'Screen navigation', 'shot': 'Screenshot'},
    'vi': {'class': 'Sơ đồ lớp', 'erd': 'Sơ đồ ERD', 'state': 'Sơ đồ trạng thái', 'sequence': 'Sơ đồ tuần tự',
           'activity': 'Sơ đồ hoạt động', 'dfd': 'Sơ đồ luồng dữ liệu', 'wireframe': 'Wireframe', 'use': 'Sơ đồ use case',
           'functional': 'Sơ đồ phân rã chức năng', 'screen': 'Sơ đồ điều hướng màn hình', 'shot': 'Màn hình'},
}[LANG]
SUBJECTS = {'table reservation': 'Table & Reservation', 'order kitchen': 'Order & Kitchen', 'billing': 'Billing & Reporting'}


def humanize(path):
    """'w8-state-table.png' -> 'State diagram: table'; a screenshot -> 'Screenshot: floor'."""
    words = [w for w in re.sub(r'^(w\d+-|\d+-)+', '', path.stem).split('-') if not re.fullmatch(r'\d+[a-z]?', w)]
    if 'screenshots' in path.as_posix():
        return f"{KINDS['shot']}: {' '.join(words)}"
    kind = words[0] if words else ''
    if kind in ('use', 'functional', 'screen'):
        return KINDS[kind]
    rest = ' '.join(words[1:])
    rest = SUBJECTS.get(rest, rest)
    if kind == 'dfd':
        level = re.search(r'dfd-(\d)', path.stem)
        rest = (f'level {level.group(1)}' if level else '') + (f', {rest}' if rest else '')
    name = KINDS.get(kind, kind.capitalize())
    return f'{name}: {rest}' if rest else name


def figures_in(text, owner):
    """Replaces each image path with 'Figure n' and returns the new text and the images to embed after it."""
    embeds = []

    def repl(m):
        path = resolve_image(m.group(1), owner)
        if not path:
            return m.group(0)
        key = path.resolve()
        if key not in IMAGES:
            # figures are placed in the order they are first named, so this matches the SEQ number Word gives
            IMAGES[key] = len(IMAGES) + 1
            embeds.append(path)
        return f'{T["figure"]} {IMAGES[key]}'
    text = re.sub(r'(?:Figures? 3\.x,?\s*)?\(?`([^`]+\.(?:png|jpe?g))`\)?', repl, text)
    return text, embeds


def place_image(path):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    spacing(p, before=6, after=0, line=1.0, keep_next=True)
    width = Inches(6.0)
    from PIL import Image
    with Image.open(path) as im:
        w, h = im.size
    if h / w > 1.1:       # tall images: limit the height instead
        width = Inches(min(6.0, 7.0 * w / h))
    p.add_run().add_picture(str(path), width=width)
    caption('Figure', humanize(path))


def set_cell_borders(table):
    tbl_pr = table._tbl.tblPr
    borders = OxmlElement('w:tblBorders')
    for edge in ('top', 'left', 'bottom', 'right', 'insideH', 'insideV'):
        el = OxmlElement(f'w:{edge}')
        el.set(qn('w:val'), 'single')
        el.set(qn('w:sz'), '4')
        el.set(qn('w:color'), '808080')
        borders.append(el)
    tbl_pr.append(borders)


def shade(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    sh = OxmlElement('w:shd')
    sh.set(qn('w:val'), 'clear')
    sh.set(qn('w:color'), 'auto')
    sh.set(qn('w:fill'), fill)
    tc_pr.append(sh)


def split_row(line):
    cells = re.split(r'(?<!\\)\|', line.strip().strip('|'))
    return [c.strip() for c in cells]


def add_table(rows, title, owner, width_in=6.5, size=10):
    header, data = split_row(rows[0]), [split_row(r) for r in rows[2:]]
    ncols = len(header)
    data = [(r + [''] * ncols)[:ncols] for r in data]
    caption('Table', title)
    t = doc.add_table(rows=1 + len(data), cols=ncols)
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_cell_borders(t)
    lengths = [max(len(header[c]), *(min(len(r[c]), 160) for r in data), 4) for c in range(ncols)]
    total = sum(l ** 0.8 for l in lengths)
    raw = [max(width_in * (l ** 0.8) / total, min(0.95, width_in / ncols)) for l in lengths]   # no column narrower than an ID
    widths = [Inches(w * width_in / sum(raw)) for w in raw]
    t.autofit = False                      # fixed layout: Word keeps these widths instead of shrinking the ID column
    for ci, col in enumerate(t.columns):
        col.width = widths[ci]
    pending = []
    for ri, cells in enumerate([header] + data):
        row = t.rows[ri]
        for ci, value in enumerate(cells):
            cell = row.cells[ci]
            cell.width = widths[ci]
            cp = cell.paragraphs[0]
            spacing(cp, before=1, after=1, line=1.1)
            value, embeds = figures_in(value, owner)
            pending += embeds
            add_inline(cp, value, size=size, bold=(ri == 0))
            if ri == 0:
                shade(cell, 'E7E5E4')
    after = doc.add_paragraph()
    spacing(after, after=4, line=1.0)
    for path in pending:
        place_image(path)


# ---------------------------------------------------------------- Markdown blocks -> document

def render(lines, owner=None, heading_base=None, table_title=''):
    """Paragraphs, lists, tables and code blocks. heading_base maps '###' to a Word heading level."""
    i, current_title = 0, table_title
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        if not stripped or stripped == '---':
            i += 1
            continue
        h = re.match(r'^(#+)\s+(.*)$', line)
        if h:
            level = min(4, max(2, len(h.group(1)) - (heading_base or 0)))
            heading(h.group(2).strip(), level)
            current_title = h.group(2).strip()
            i += 1
            continue
        if stripped.startswith('```'):
            code = []
            i += 1
            while i < len(lines) and not lines[i].strip().startswith('```'):
                code.append(lines[i])
                i += 1
            i += 1
            for c in code:
                p = doc.add_paragraph()
                spacing(p, after=0, line=1.0)
                p.paragraph_format.left_indent = Inches(0.3)
                r = p.add_run(c if c else ' ')
                set_run_font(r, 9.5, mono=True)
            doc.add_paragraph()
            continue
        if stripped.startswith('|'):
            rows = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                rows.append(lines[i])
                i += 1
            add_table(rows, current_title, owner)
            continue
        m = re.match(r'^(\s*)([-*]|\d+\.)\s+(.*)$', line)
        if m:
            indent = len(m.group(1)) // 2
            text, embeds = figures_in(m.group(3), owner)
            p = doc.add_paragraph()
            spacing(p, after=2)
            if m.group(2) in ('-', '*'):
                p_pr = p._p.get_or_add_pPr()
                num = OxmlElement('w:numPr')
                ilvl = OxmlElement('w:ilvl')
                ilvl.set(qn('w:val'), str(min(indent, 2)))
                nid = OxmlElement('w:numId')
                nid.set(qn('w:val'), '1')
                num.append(ilvl)
                num.append(nid)
                p_pr.insert(0, num)
                p.paragraph_format.left_indent = Inches(0.35 + 0.3 * indent)
                p.paragraph_format.first_line_indent = Inches(-0.25)
                add_inline(p, text)
            else:
                p.paragraph_format.left_indent = Inches(0.35 + 0.3 * indent)
                p.paragraph_format.first_line_indent = Inches(-0.25)
                add_inline(p, f'{m.group(2)} {text}')
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            for path in embeds:
                place_image(path)
            i += 1
            continue
        # plain paragraph: join soft-wrapped lines
        para = [stripped]
        i += 1
        while i < len(lines) and lines[i].strip() and not re.match(r'^(#|\s*[-*]\s|\s*\d+\.\s|\s*\||\s*```)', lines[i]):
            para.append(lines[i].strip())
            i += 1
        text, embeds = figures_in(' '.join(para), owner)
        p = doc.add_paragraph()
        spacing(p)
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        add_inline(p, text)
        for path in embeds:
            place_image(path)


# ---------------------------------------------------------------- 1. cover, declaration, contents

def replace_in_paragraph(p_el, old, new):
    ts = list(p_el.iter(qn('w:t')))
    full = ''.join(t.text or '' for t in ts)
    if old not in full or not ts:
        return False
    ts[0].text = full.replace(old, new)
    for t in ts[1:]:
        t.text = ''
    return True


for tb in body.iter(qn('w:txbxContent')):
    paras = list(tb.iter(qn('w:p')))
    for p_el in paras:
        replace_in_paragraph(p_el, 'PROJECT TITLE', T['title'])
        if LANG == 'vi':
            replace_in_paragraph(p_el, 'APPLICATION PROGRAMMING PROJECT REPORT', 'BÁO CÁO ĐỒ ÁN LẬP TRÌNH ỨNG DỤNG')
            replace_in_paragraph(p_el, 'VAN LANG UNIVERSITY', 'TRƯỜNG ĐẠI HỌC VĂN LANG')
            replace_in_paragraph(p_el, 'FACULTY OF INFORMATION TECHNOLOGY', 'KHOA CÔNG NGHỆ THÔNG TIN')
            replace_in_paragraph(p_el, 'Title: ', 'Đề tài: ')
            replace_in_paragraph(p_el, 'Advisor: Dr. Nguyen Tri Hai', 'Giảng viên hướng dẫn: TS. Nguyễn Trí Hải')
    # bottom box: course code and class group, then city and submission date (Guidelines §8, cover page)
    for i, p_el in enumerate(paras):
        if replace_in_paragraph(p_el, 'Ho Chi Minh City – 2026', T['course']) and i + 1 < len(paras):
            date = copy.deepcopy(p_el)
            replace_in_paragraph(date, T['course'], f'{T["city"]} – {SUBMIT_DATE}')
            paras[i + 1].getparent().replace(paras[i + 1], date)
    texts = [''.join(t.text or '' for t in p.iter(qn('w:t'))) for p in paras]
    if texts and texts[0].startswith('Presented by'):
        presented, student = paras[0], paras[1]
        replace_in_paragraph(presented, 'Presented by: (Your Full Name)', T['presented'])
        replace_in_paragraph(student, 'Student ID: ….', T['members'][0])
        anchor = student
        for member in T['members'][1:]:
            clone = copy.deepcopy(student)
            replace_in_paragraph(clone, T['members'][0], member)
            anchor.addnext(clone)
            anchor = clone
        for p_el in tb.iter(qn('w:p')):
            for sz in list(p_el.iter(qn('w:sz'))) + list(p_el.iter(qn('w:szCs'))):
                sz.set(qn('w:val'), '26')

# the cover box with the names needs room for three members and the advisor
for shape in body.iter(qn('wp:anchor')):
    if 'Advisor' in ''.join(t.text or '' for t in shape.iter(qn('w:t'))) or 'Giảng viên' in ''.join(t.text or '' for t in shape.iter(qn('w:t'))):
        for ext in shape.iter():
            if ext.tag.endswith('}extent') or (ext.tag.endswith('}ext') and ext.get('cy')):
                ext.set('cy', str(int(int(ext.get('cy')) * 1.7)))
# the bottom box now holds two lines (course and class group, city and date): wider, taller, moved up and left
EMU_PT = 12700
for shape in body.iter(qn('wp:anchor')):
    extent = shape.find(qn('wp:extent'))
    if extent is not None and extent.get('cx') == '3340100' and T['course'] in ''.join(t.text or '' for t in shape.iter(qn('w:t'))):
        for ext in shape.iter():
            if ext.tag.endswith('}extent') or (ext.tag.endswith('}ext') and ext.get('cx')):
                ext.set('cx', str(400 * EMU_PT))
                ext.set('cy', str(int(63.3 * EMU_PT)))
        for axis, shift in (('wp:positionH', 68.5), ('wp:positionV', 31.65)):
            off = shape.find(qn(axis)).find(qn('wp:posOffset'))
            if off is not None:
                off.text = str(int(off.text) - int(shift * EMU_PT))
for el in body.iter():
    style = el.get('style')
    if style and 'width:263pt;height:31.65pt' in style:
        el.set('style', style.replace('width:263pt;height:31.65pt', 'width:400pt;height:63.3pt'))
    if style and 'height:112.35pt' in style:
        el.set('style', style.replace('height:112.35pt', 'height:191pt'))

paragraphs = doc.paragraphs
decl = {
    'en': [('I, (Your Full Name)', 'We, ' + T['names_ids']), ('(Your Full Name)', T['signers']), ('(Your Project Title)', 'Restaurant Table Booking and Kitchen Order System'),
           ('my own work', 'our own work'), ('I confirm', 'We confirm'),
           ('original authors. I understand', 'original authors, and that our use of AI tools is declared in Appendix B. We understand'),
           ('Date:……/……/…………', 'Date: ' + SUBMIT_DATE), ('Signature of Student', 'Signatures of Students')],
    'vi': [],
}[LANG]
for p in paragraphs[28:40]:
    for old, new in decl:
        replace_in_paragraph(p._p, old, new)
if LANG == 'vi':
    vi_decl = {
        28: 'CAM KẾT KHÔNG ĐẠO VĂN',
        29: f'Chúng tôi, {T["names_ids"]}, cam kết toàn bộ nội dung trong báo cáo "Hệ thống đặt bàn và gọi món cho nhà hàng", nộp theo yêu cầu của Khoa Công nghệ Thông tin, Trường Công nghệ Văn Lang, Trường Đại học Văn Lang, là kết quả làm việc của chính chúng tôi và không đạo văn.',
        31: 'Chúng tôi xác nhận mọi ý tưởng lấy từ nguồn in (sách, tạp chí) hoặc nguồn điện tử (website) đều đã được ghi nguồn tác giả gốc, và việc sử dụng công cụ AI đã được khai báo ở Phụ lục B. Chúng tôi hiểu rằng nếu không làm như vậy, báo cáo có thể bị từ chối.',
        33: f'TP. Hồ Chí Minh, ngày {SUBMIT_DATE}',
        34: 'Chữ ký của sinh viên',
        38: T['signers'],
        40: 'MỤC LỤC', 42: 'DANH MỤC HÌNH', 45: 'DANH MỤC BẢNG',
    }
    for idx, text in vi_decl.items():
        ts = list(paragraphs[idx]._p.iter(qn('w:t')))
        if ts:
            ts[0].text = text
            for t in ts[1:]:
                t.text = ''

# contents, list of figures, list of tables: Word fills these fields when export_pdf.ps1 runs
field(paragraphs[41], 'TOC \\o "1-3" \\h \\z \\u', 'Right-click and choose Update Field.', first=True)
for idx, kind in ((43, 'Figure'), (46, 'Table')):
    for t in paragraphs[idx]._p.iter(qn('w:t')):
        t.text = ''
    field(paragraphs[idx], f'TOC \\h \\z \\c "{kind}"', ' ', first=True)

# drop the template's chapter outline (from CHAPTER 1 to the end), keep the final section properties
for p in paragraphs[47:]:
    p._p.getparent().remove(p._p)

# ---------------------------------------------------------------- 2. chapters

giang, lam, khoa = (read(f'docs/report/{n}.md') for n in ('giang', 'lam', 'khoa'))
proposal = read('docs/proposal/project-proposal-draft.md')

page_break()
heading(T['ch'][0], 1)
render(section(giang, r'^Chapter 1|^Chương 1'), owner='giang', heading_base=1)

page_break()
heading(T['ch'][1], 1)
render(section(lam, r'^Chapter 2|^Chương 2'), owner='lam', heading_base=1)

page_break()
heading(T['ch'][2], 1)
parts = {n: numbered_parts(section(src, r'^Chapter 3|^Chương 3')) for n, src in (('giang', giang), ('lam', lam), ('khoa', khoa))}
for key, title, level in T['ch3']:
    heading(title, level)
    for owner in ('giang', 'lam', 'khoa'):
        content = [l for l in parts[owner].get(key, []) if l.strip()]
        if content:
            heading(T['modules'][owner], 4)
            render(parts[owner][key], owner=owner, table_title=f'{title} – {T["modules"][owner].split(" (")[0]}')

page_break()
heading(T['ch'][3], 1)
ch4 = section(giang, r'^Chapter 4|^Chương 4')
# 4.2: add Lam's and Khoa's rows to Giang's skills table
skills_rows = []
for src in (lam, khoa):
    skills_rows += table_rows(section(src, r'^Chapter 4 rows|^Các dòng Chương 4')[
        [i for i, l in enumerate(section(src, r'^Chapter 4 rows|^Các dòng Chương 4')) if '4.2' in l][0]:])
idx = max(i for i, l in enumerate(ch4) if l.strip().startswith('|'))
ch4 = ch4[:idx + 1] + skills_rows + ch4[idx + 1:]
render(ch4, owner='giang', heading_base=1)

# ---------------------------------------------------------------- 3. appendices and references

page_break()
heading(T['appendices'], 1)
heading(T['appA'], 2)
render([T['appA_note']])
uc_vi = REPORT / 'appendix-a.vi.md'
uc = (uc_vi.read_text(encoding='utf-8').splitlines() if LANG == 'vi' and uc_vi.exists()
      else section(proposal, r'^Appendix A'))
render([re.sub(r'^###', '####', l) for l in uc], owner='giang', heading_base=1)

page_break()
heading(T['appB'], 2)
ai_rows = []
for src in (giang, lam, khoa):
    rows = [l for l in section(src, r'^Appendix B|^Phụ lục B') if l.strip().startswith('|')]
    if not ai_rows:
        ai_rows = rows[:2]
    ai_rows += rows[2:]
render(ai_rows, table_title=T['appB'])
render([T['declare']])

heading(T['appC'], 2)
c_rows = []
for src in (giang, lam, khoa):
    rows = [l for l in section(src, r'^Appendix C|^Phụ lục C') if l.strip().startswith('|')]
    if not c_rows:
        c_rows = rows[:2]
    c_rows += rows[2:]
render(c_rows, table_title=T['appC'])
render([T['appC_note']])

# Appendix D: the eight-column test-case tables in smaller type
page_break()                      # portrait like the rest: mixed page sizes look uneven in a PDF viewer
heading(T['appD'], 2)
if T.get('appD_note'):
    render([T['appD_note']])
for path, title in T['testlogs']:
    heading(title, 3)
    lines = read(path).split('\n')
    lines = [l for l in lines if not l.startswith('# ')]
    i = 0
    while i < len(lines):
        if lines[i].strip().startswith('|'):
            rows = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                rows.append(lines[i])
                i += 1
            add_table(rows, title, None, width_in=6.5, size=7.5)
        else:
            chunk = []
            while i < len(lines) and not lines[i].strip().startswith('|'):
                chunk.append(lines[i])
                i += 1
            render([re.sub(r'^#+', '####', l) for l in chunk])

page_break()
heading(T['references'], 1)
refs = [l for l in section(proposal, r'^References') if l.strip() and l.strip() != '---']
joined, cur = [], ''
for l in refs:
    if re.match(r'^\[\d+\]', l.strip()):
        if cur:
            joined.append(cur)
        cur = l.strip()
    else:
        cur += ' ' + l.strip()
joined.append(cur)
for ref in joined:
    p = doc.add_paragraph()
    spacing(p, after=4)
    p.paragraph_format.left_indent = Inches(0.45)
    p.paragraph_format.first_line_indent = Inches(-0.45)
    add_inline(p, ref)

OUT.parent.mkdir(parents=True, exist_ok=True)
# plain hyphens only: no en dash or em dash anywhere in the report (cover, text boxes, captions, tables)
for t in doc.element.body.iter(qn('w:t')):
    if t.text and ('–' in t.text or '—' in t.text):
        t.text = t.text.replace('—', '-').replace('–', '-')
doc.save(str(OUT))
print(f'{OUT}  figures={counters["Figure"]} tables={counters["Table"]}')
