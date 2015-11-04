package jp.hf.bay_admin.controller;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.hf.bay_admin.commons.AdminUtil;
import jp.hf.bay_admin.commons.Const;
import jp.hf.bay_admin.exception.FieldException;
import jp.hf.bay_admin.model.PickupBase;
import jp.hf.bay_admin.model.PickupEntityModule;
import jp.hf.bay_admin.model.form.PickupForm;
import jp.hf.bay_admin.model.form.PickupEditForm;

@Controller
@RequestMapping("/pickup")
@SessionAttributes(types = { PickupForm.class, PickupEditForm.class })
public class PickupController {

	@Autowired
	private PickupBase pickupBase;

	@ModelAttribute("pickupForm")
	public PickupForm setUpPickupForm() {
		return new PickupForm();
	}

	@ModelAttribute("pickupEditForm")
	public PickupEditForm setUpPickupEditForm() {
		return new PickupEditForm();
	}

	@RequestMapping(value = "list", method = RequestMethod.GET)
	public String showPickupList(@ModelAttribute("pickupForm") PickupForm form, Model model, SessionStatus sessionStatus) {
		sessionStatus.setComplete();
		// 共通処理へ
		return showPickupLinkSelectedList(form, Const.PICKUP_UPPER_VAL, null, model);
	}

	@RequestMapping(value = "list/page", method = RequestMethod.GET)
	public String showPickupListFromPage(@ModelAttribute("pickupForm") PickupForm form, Model model) {
		// 共通処理へ
		return showPickupLinkSelectedList(form, form.getUpperLower(), form.getPastDataFlg(), model);
	}

	@RequestMapping(value = "list/linkselected", method = RequestMethod.POST)
	public String showPickupLinkSelectedList(@ModelAttribute("pickupForm") PickupForm form, String upperLower, String pastDataFlg, Model model) {

		// リクエストがPOSTの場合は先頭ページを表示
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		HttpServletRequest req = (HttpServletRequest) attrs.resolveReference(RequestAttributes.REFERENCE_REQUEST);
		if (req.getMethod().equals(Const.REQUEST_TYPE_POST)) {
			form.setPage(0);
		}

		// 「掲載終了も表示する」のチェック設定
		boolean flg = false;
		if(StringUtils.isNotEmpty(pastDataFlg) && pastDataFlg.equals(Const.PICKUP_LIST_CHECK_VAL)) {
			flg = true;
		}

		// Modelのパラメータ設定
		model.addAttribute("lists", pickupBase.list(form, upperLower, flg));
		model.addAttribute("pages", pickupBase.getListPage(form, upperLower, flg));
		model.addAttribute("pastDataFlg", pastDataFlg);
		// フォームのパラメータ設定
		form.setUpperLower(upperLower);
		form.setPastDataFlg(pastDataFlg);
		return "pickup/list";
	}

	@RequestMapping(value = "list/delete", method = RequestMethod.POST)
	public String doDelete(@ModelAttribute("pickupForm") PickupForm form, String delId, Principal principal, Model model) {
		// 削除実行
		if(StringUtils.isNotEmpty(delId)) {
			pickupBase.delete(Integer.parseInt(delId), AdminUtil.loginInfo(principal));
		}
		// 共通処理へ
		return showPickupLinkSelectedList(form, form.getUpperLower(), form.getPastDataFlg(), model);
	}

	@RequestMapping(value = "list/reDo", method = RequestMethod.POST)
	public String reDoForm() {
		return "redirect:/pickup/list";
	}

	@RequestMapping(value = "add", method = RequestMethod.POST)
	public String redirectToAddForm(@ModelAttribute("pickupEditForm") PickupEditForm form, RedirectAttributes redirectAttributes, Model model) {
		// フォームの設定
		form.setStartYearVal(pickupBase.getNowDateParts().get(Const.PICKUP_CARRING_YEAR));
		form.setStartMonthVal(pickupBase.getNowDateParts().get(Const.PICKUP_CARRING_MONTH));
		form.setStartDayVal(pickupBase.getNowDateParts().get(Const.PICKUP_CARRING_DAY));
		form.setStartHourVal(pickupBase.getNowDateParts().get(Const.PICKUP_CARRING_HOUR));
		form.setStartMinuteVal(pickupBase.getNowDateParts().get(Const.PICKUP_CARRING_MINUTE));
		redirectAttributes.addFlashAttribute("pickupEditForm", form);
		return "redirect:/pickup/add";
	}

	@RequestMapping(value = "/add", method = RequestMethod.GET)
	public String showAddForm(@ModelAttribute("pickupEditForm") PickupEditForm form, Model model) {
		// 画像設定
		form.setDispImg(Const.PICKUP_DEFAULT_IMAGE);
    	// 各リスト設定
		setModelToFormControl(form, model);
		return "pickup/add";
	}

    @RequestMapping(value = "add/exec", method = RequestMethod.POST)
    public String addExec(@Validated @ModelAttribute("pickupEditForm") PickupEditForm form, BindingResult result,
    		Principal principal, RedirectAttributes redirectAttributes, Model model) throws FieldException, Exception  {

    	// エラーメッセージクリア
    	form.setImgErrorMsg("");

    	// 入力項目チェック
    	if (result.hasFieldErrors()) {
    		form.setImgErrorMsg(Const.PICKUP_MSG_RELOAD_IMG);
    		setModelToFormControl(form, model);
			return "pickup/add";
    	}

    	// 画像選択チェック
    	MultipartFile uploadFile = form.getFile();
    	if (uploadFile == null || uploadFile.isEmpty()) {
    		form.setImgErrorMsg(Const.PICKUP_MSG_ERROR_IMG);
    		setModelToFormControl(form, model);
    		return "pickup/add";
    	}

    	// 画像一時保存
    	form.setDispImg(Const.PICKUP_IMG_TMP_PATH + pickupBase.saveTmpImgFile(form));

    	// 表示位置設定
    	String posiText;
    	if(form.getPosiVal().equals(Const.PICKUP_UPPER_VAL)) {
    		posiText = Const.PICKUP_UPPER_NAME;
    	} else {
    		posiText = Const.PICKUP_LOWER_NAME;
    	}

    	// タグ名設定
    	String tagText = "";
    	if(form.getTagId() > 0) {
    		tagText = pickupBase.getTagName(form.getTagId());
    	}

    	// RedirectAttributesのパラメータ設定
		redirectAttributes.addFlashAttribute("posiText", posiText);
		redirectAttributes.addFlashAttribute("tagText", tagText);
		redirectAttributes.addFlashAttribute("startDateText", pickupBase.createDateString(form, true));
		redirectAttributes.addFlashAttribute("endDateText", pickupBase.createDateString(form, false));
		redirectAttributes.addFlashAttribute("pickupEditForm", form);
		redirectAttributes.addFlashAttribute("cmdFlg", "add");
    	return "redirect:/pickup/confirm";
    }

	@RequestMapping(value = "/confirm", method = RequestMethod.GET)
	public String showConfirm() {
		return "pickup/confirm";
	}

	@RequestMapping(value = "/confirm", params = "_cmd_add_redo", method = RequestMethod.POST)
	public String addReDo(@ModelAttribute("pickupEditForm") PickupEditForm form, RedirectAttributes redirectAttributes, Errors errors) {
		// フォームのコントロール設定
		setRedirectAttributesToFormControl(redirectAttributes);
		// RedirectAttributesのパラメータ設定
		form.setDispImg(Const.PICKUP_DEFAULT_IMAGE);
		form.setImgErrorMsg(Const.PICKUP_MSG_RELOAD_IMG);
		redirectAttributes.addFlashAttribute("pickupEditForm", form);
		return "redirect:/pickup/add";
	}

	@RequestMapping(value = "/confirm", params = "_cmd_add", method = RequestMethod.POST)
	public String add(@ModelAttribute("pickupEditForm") PickupEditForm form, Principal principal, RedirectAttributes redirectAttributes) throws IOException {
		// 登録処理
		pickupBase.add(form, AdminUtil.loginInfo(principal));
		// 画像保存
		pickupBase.saveImgFile(form);
		// RedirectAttributesのパラメータ設定
		redirectAttributes.addFlashAttribute("cmd_msg", Const.PICKUP_MSG_ADD);
		return "redirect:/pickup/list";
	}

	@RequestMapping(value = "/confirm", params = "_cmd_edit_redo", method = RequestMethod.POST)
	public String editReDo(@ModelAttribute("pickupEditForm") PickupEditForm form, RedirectAttributes redirectAttributes, Errors errors) {
		// フォームのコントロール設定
		setRedirectAttributesToFormControl(redirectAttributes);
		// RedirectAttributesのパラメータ設定
		if(form.getEditDelFlg()) {
			form.setDispImg(Const.PICKUP_DEFAULT_IMAGE);
			form.setImgErrorMsg(Const.PICKUP_MSG_RELOAD_IMG);
		}
		redirectAttributes.addFlashAttribute("pickupEditForm", form);
		return "redirect:/pickup/edit";
	}

	@RequestMapping(value = "/confirm", params = "_cmd_edit", method = RequestMethod.POST)
	public String edit(@ModelAttribute("pickupEditForm") PickupEditForm form, Principal principal, RedirectAttributes redirectAttributes) throws IOException {
		// 登録処理
		pickupBase.update(form, AdminUtil.loginInfo(principal));
		// 画像保存
		if(form.getEditDelFlg()) {
			pickupBase.saveImgFile(form);
		}
		// RedirectAttributesのパラメータ設定
		redirectAttributes.addFlashAttribute("cmd_msg", Const.PICKUP_MSG_UPDATE);
		return "redirect:/pickup/list";
	}

	@RequestMapping(value = "edit/{id}", method = RequestMethod.POST)
	public String redirectToEditForm(@PathVariable String id, @ModelAttribute("pickupEditForm") PickupEditForm form, RedirectAttributes redirectAttributes) {
		// フォームの設定
		setEditFormParameter(form, id);
		redirectAttributes.addFlashAttribute("pickupEditForm", form);
		return "redirect:/pickup/edit";
	}

	@RequestMapping(value = "/edit", method = RequestMethod.GET)
	public String showEditForm(@ModelAttribute("pickupEditForm") PickupEditForm form, Model model) {
    	if(form.getEditDelFlg()) {
    		form.setDispImg(Const.PICKUP_DEFAULT_IMAGE);
    	}
    	// 各コントロール設定
		setModelToFormControl(form, model);
		return "pickup/edit";
	}

	@RequestMapping(value = "edit/delete", method = RequestMethod.POST)
	public String doEditImgDelete(@ModelAttribute("pickupEditForm") PickupEditForm form, RedirectAttributes redirectAttributes) {
    	// 各コントロール設定
		setRedirectAttributesToFormControl(redirectAttributes);
		// フォームの設定
		form.setDispImg(Const.PICKUP_DEFAULT_IMAGE);
		form.setEditDelFlg(true);
		redirectAttributes.addFlashAttribute("pickupEditForm", form);
		return "redirect:/pickup/edit";
	}

    @RequestMapping(value = "edit/exec", method = RequestMethod.POST)
    public String editExec(@Validated @ModelAttribute("pickupEditForm") PickupEditForm form, BindingResult result,
    		Principal principal, RedirectAttributes redirectAttributes, Model model) throws FieldException, Exception  {

    	// エラーメッセージクリア
    	form.setImgErrorMsg("");

    	// 入力項目チェック
    	if (result.hasFieldErrors()) {
   			form.setImgErrorMsg(Const.PICKUP_MSG_RELOAD_IMG);
    		setModelToFormControl(form, model);
			return "pickup/edit";
    	}

    	// 画像選択チェック
    	boolean bErr = false;
    	if(form.getEditDelFlg()) {
	    	MultipartFile uploadFile = form.getFile();
	    	if (uploadFile == null || uploadFile.isEmpty()) {
    			bErr = true;
	    	}
    	} else {
    		if(form.getDispImg().equals(Const.PICKUP_DEFAULT_IMAGE)) {
    			bErr = true;
    		}
    	}
    	if(bErr) {
    		form.setImgErrorMsg(Const.PICKUP_MSG_ERROR_IMG);
    		setModelToFormControl(form, model);
    		return "pickup/edit";
    	}

    	// 画像一時保存
    	if(form.getEditDelFlg()) {
    		form.setDispImg(Const.PICKUP_IMG_TMP_PATH + pickupBase.saveTmpImgFile(form));
    	}

    	// 表示位置設定
    	String posiText;
    	if(form.getPosiVal().equals(Const.PICKUP_UPPER_VAL)) {
    		posiText = Const.PICKUP_UPPER_NAME;
    	} else {
    		posiText = Const.PICKUP_LOWER_NAME;
    	}

    	// タグ名設定
    	String tagText = "";
    	if(form.getTagId() > 0) {
    		tagText = pickupBase.getTagName(form.getTagId());
    	}

    	// RedirectAttributesのパラメータ設定
		redirectAttributes.addFlashAttribute("posiText", posiText);
		redirectAttributes.addFlashAttribute("tagText", tagText);
		redirectAttributes.addFlashAttribute("startDateText", pickupBase.createDateString(form, true));
		redirectAttributes.addFlashAttribute("endDateText", pickupBase.createDateString(form, false));
		redirectAttributes.addFlashAttribute("pickupEditForm", form);
		redirectAttributes.addFlashAttribute("cmdFlg", "edit");
    	return "redirect:/pickup/confirm";
    }

	private void setRedirectAttributesToFormControl(RedirectAttributes redirectAttributes) {
    	// 各コントロール設定
		redirectAttributes.addFlashAttribute("posiSelect", pickupBase.createPosiSelectList());
		redirectAttributes.addFlashAttribute("orderSelect", pickupBase.createOrderSelectList());
		redirectAttributes.addFlashAttribute("tagSelect", pickupBase.getTagList());
		redirectAttributes.addFlashAttribute("startYearSelect", pickupBase.createYearList());
		redirectAttributes.addFlashAttribute("startMonthSelect", pickupBase.createMonthList());
		redirectAttributes.addFlashAttribute("startDaySelect", pickupBase.createDayList());
		redirectAttributes.addFlashAttribute("startHourSelect", pickupBase.createHourList());
		redirectAttributes.addFlashAttribute("startMinuteSelect", pickupBase.createMinuteList());
		redirectAttributes.addFlashAttribute("endYearSelect", pickupBase.createYearList());
		redirectAttributes.addFlashAttribute("endMonthSelect", pickupBase.createMonthList());
		redirectAttributes.addFlashAttribute("endDaySelect", pickupBase.createDayList());
		redirectAttributes.addFlashAttribute("endHourSelect", pickupBase.createHourList());
		redirectAttributes.addFlashAttribute("endMinuteSelect", pickupBase.createMinuteList());
	}

    private void setModelToFormControl(PickupEditForm form, Model model) {
    	// 各コントロール設定
		model.addAttribute("posiSelect", pickupBase.createPosiSelectList());
		model.addAttribute("orderSelect", pickupBase.createOrderSelectList());
		model.addAttribute("tagSelect", pickupBase.getTagList());
		model.addAttribute("startYearSelect", pickupBase.createYearList());
		model.addAttribute("startMonthSelect", pickupBase.createMonthList());
		model.addAttribute("startDaySelect", pickupBase.createDayList());
		model.addAttribute("startHourSelect", pickupBase.createHourList());
		model.addAttribute("startMinuteSelect", pickupBase.createMinuteList());
		model.addAttribute("endYearSelect", pickupBase.createYearList());
		model.addAttribute("endMonthSelect", pickupBase.createMonthList());
		model.addAttribute("endDaySelect", pickupBase.createDayList());
		model.addAttribute("endHourSelect", pickupBase.createHourList());
		model.addAttribute("endMinuteSelect", pickupBase.createMinuteList());
		// フォームの設定
		model.addAttribute("pickupEditForm", form);
    }

    private void setEditFormParameter(PickupEditForm form, String id) {
    	PickupEntityModule module = pickupBase.getPickupData(Long.parseLong(id));
    	form.setEditId(Long.parseLong(id));
    	form.setDispImg(module.getImgPath());
    	form.setEditPickupUrl(module.getUrl());
    	form.setEditPickupTitle(module.getTitle());
    	form.setEditPickupNote(module.getNote());
    	form.setPosiVal(module.getDispPosition());
    	form.setOrderVal(module.getDispOrder());
    	form.setTagId(module.getPickupTagId());
    	form.setStartYearVal(pickupBase.getEditYear(module.getCarryingStartTime()));
    	form.setStartMonthVal(pickupBase.getEditMonth(module.getCarryingStartTime()));
    	form.setStartDayVal(pickupBase.getEditDay(module.getCarryingStartTime()));
    	form.setStartHourVal(pickupBase.getEditHour(module.getCarryingStartTime()));
    	form.setStartMinuteVal(pickupBase.getEditMinute(module.getCarryingStartTime()));
    	if(module.getCarryingEndTime() != null) {
        	form.setEndYearVal(pickupBase.getEditYear(module.getCarryingEndTime()));
        	form.setEndMonthVal(pickupBase.getEditMonth(module.getCarryingEndTime()));
        	form.setEndDayVal(pickupBase.getEditDay(module.getCarryingEndTime()));
        	form.setEndHourVal(pickupBase.getEditHour(module.getCarryingEndTime()));
        	form.setEndMinuteVal(pickupBase.getEditMinute(module.getCarryingEndTime()));
    	}
    	form.setRegistUser(module.getRegistUser());
    	form.setRegistDate(module.getRegistDate());
    	form.setEditDelFlg(false);
    }
}

